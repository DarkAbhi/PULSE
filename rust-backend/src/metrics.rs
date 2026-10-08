use std::{sync::Arc, time::Instant};

use axum::{
    Router,
    extract::{MatchedPath, Request, State},
    http::{StatusCode, header},
    middleware::Next,
    response::Response,
    routing::get,
};
use prometheus::{
    Encoder, HistogramOpts, HistogramVec, IntCounterVec, IntGauge, IntGaugeVec, Opts, Registry,
    TextEncoder,
};
use sqlx::PgPool;

pub struct Metrics {
    registry: Registry,
    requests: IntCounterVec,
    duration: HistogramVec,
    in_flight: IntGaugeVec,
    total: IntGauge,
    idle: IntGauge,
    acquired: IntGauge,
    max: IntGauge,
}

impl Metrics {
    pub fn new() -> Result<Arc<Self>, prometheus::Error> {
        let metrics = Self {
            registry: Registry::new(),
            requests: IntCounterVec::new(
                Opts::new("life_http_requests_total", "Completed HTTP requests"),
                &["method", "path", "status"],
            )?,
            duration: HistogramVec::new(
                HistogramOpts::new(
                    "life_http_request_duration_seconds",
                    "HTTP handler duration in seconds",
                )
                .buckets(vec![
                    0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0,
                ]),
                &["method", "path", "status"],
            )?,
            in_flight: IntGaugeVec::new(
                Opts::new("life_http_requests_in_flight", "Active HTTP handlers"),
                &["method"],
            )?,
            total: IntGauge::new(
                "life_db_pool_total_connections",
                "Established pool connections",
            )?,
            idle: IntGauge::new("life_db_pool_idle_connections", "Idle pool connections")?,
            acquired: IntGauge::new(
                "life_db_pool_acquired_connections",
                "Pool connections in use",
            )?,
            max: IntGauge::new("life_db_pool_max_connections", "Configured pool capacity")?,
        };
        metrics
            .registry
            .register(Box::new(metrics.requests.clone()))?;
        metrics
            .registry
            .register(Box::new(metrics.duration.clone()))?;
        metrics
            .registry
            .register(Box::new(metrics.in_flight.clone()))?;
        metrics.registry.register(Box::new(metrics.total.clone()))?;
        metrics.registry.register(Box::new(metrics.idle.clone()))?;
        metrics
            .registry
            .register(Box::new(metrics.acquired.clone()))?;
        metrics.registry.register(Box::new(metrics.max.clone()))?;
        Ok(Arc::new(metrics))
    }

    pub fn router(self: Arc<Self>, pool: PgPool) -> Router {
        Router::new()
            .route("/metrics", get(export))
            .with_state((self, pool))
    }
}

// Balance the gauge even if a handler future is cancelled.
struct InFlight(IntGauge);

impl Drop for InFlight {
    fn drop(&mut self) {
        self.0.dec();
    }
}

pub async fn observe(
    State(metrics): State<Arc<Metrics>>,
    request: Request,
    next: Next,
) -> Response {
    let path = request
        .extensions()
        .get::<MatchedPath>()
        .map_or("unmatched", MatchedPath::as_str)
        .to_owned();
    if matches!(path.as_str(), "/healthz" | "/readyz") {
        return next.run(request).await;
    }
    let method = request.method().as_str().to_owned();
    let gauge = metrics.in_flight.with_label_values(&[&method]);
    gauge.inc();
    let _in_flight = InFlight(gauge);
    let start = Instant::now();
    let response = next.run(request).await;
    let status = response.status().as_u16().to_string();
    let labels = [&method[..], &path[..], &status[..]];
    metrics.requests.with_label_values(&labels).inc();
    metrics
        .duration
        .with_label_values(&labels)
        .observe(start.elapsed().as_secs_f64());
    response
}

async fn export(
    State((metrics, pool)): State<(Arc<Metrics>, PgPool)>,
) -> Result<([(header::HeaderName, &'static str); 1], Vec<u8>), StatusCode> {
    let total = pool.size();
    let idle = pool.num_idle() as u32;
    metrics.total.set(i64::from(total));
    metrics.idle.set(i64::from(idle));
    metrics.acquired.set(i64::from(total.saturating_sub(idle)));
    metrics
        .max
        .set(i64::from(pool.options().get_max_connections()));
    let mut body = Vec::new();
    TextEncoder::new()
        .encode(&metrics.registry.gather(), &mut body)
        .map_err(|error| {
            tracing::error!(%error, "unable to encode metrics");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;
    Ok((
        [(
            header::CONTENT_TYPE,
            "text/plain; version=0.0.4; charset=utf-8",
        )],
        body,
    ))
}

#[cfg(test)]
mod tests {
    use super::*;
    use tokio::{
        io::{AsyncReadExt, AsyncWriteExt},
        net::{TcpListener, TcpStream},
    };

    async fn request(address: std::net::SocketAddr, path: &str) -> String {
        let mut stream = TcpStream::connect(address).await.unwrap();
        stream
            .write_all(
                format!("GET {path} HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                    .as_bytes(),
            )
            .await
            .unwrap();
        let mut body = Vec::new();
        stream.read_to_end(&mut body).await.unwrap();
        String::from_utf8(body).unwrap()
    }

    #[tokio::test]
    async fn exports_http_and_pool_metrics_without_exposing_them_on_api() {
        let pool = sqlx::postgres::PgPoolOptions::new()
            .max_connections(7)
            .connect_lazy("postgres://127.0.0.1:1/test")
            .unwrap();
        let metrics = Metrics::new().unwrap();
        let app = crate::routes::router(
            crate::state::AppState { pool: pool.clone() },
            metrics.clone(),
        );
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let api_address = listener.local_addr().unwrap();
        let api = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let metrics_address = listener.local_addr().unwrap();
        let exporter =
            tokio::spawn(async move { axum::serve(listener, metrics.router(pool)).await.unwrap() });

        request(api_address, "/api/version").await;
        request(api_address, "/healthz").await;
        assert!(
            request(api_address, "/metrics")
                .await
                .starts_with("HTTP/1.1 401")
        );
        request(api_address, "/unknown/user-secret").await;
        let body = request(metrics_address, "/metrics").await;
        assert!(body.starts_with("HTTP/1.1 200"), "{body}");
        assert!(
            body.contains(
                "life_http_requests_total{method=\"GET\",path=\"/api/version\",status=\"200\"} 1"
            ),
            "{body}"
        );
        assert!(
            body.contains(
                "life_http_requests_total{method=\"GET\",path=\"unmatched\",status=\"401\"} 2"
            ),
            "{body}"
        );
        assert!(body.contains("life_http_request_duration_seconds_count{method=\"GET\",path=\"/api/version\",status=\"200\"} 1"), "{body}");
        assert!(body.contains("life_http_requests_in_flight{method=\"GET\"} 0"));
        assert!(body.contains("life_db_pool_max_connections 7"));
        assert!(body.contains("life_db_pool_acquired_connections 0"));
        assert!(!body.contains("/healthz"));
        assert!(!body.contains("user-secret"));
        api.abort();
        exporter.abort();
    }

    #[test]
    fn in_flight_is_balanced_on_drop() {
        let gauge = IntGauge::new("test_in_flight", "test").unwrap();
        gauge.inc();
        let guard = InFlight(gauge.clone());
        assert_eq!(gauge.get(), 1);
        drop(guard);
        assert_eq!(gauge.get(), 0);
    }
}
