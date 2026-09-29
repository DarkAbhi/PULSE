use std::time::Duration;

use axum::{Json, Router, extract::State, http::StatusCode, middleware, routing::get};
use serde_json::{Value, json};
use tower_http::{cors::CorsLayer, trace::TraceLayer};

use crate::{auth, state::AppState};

pub fn router(state: AppState) -> Router {
    // Register future protected routes on this router.
    let protected = Router::<AppState>::new();
    let protected = protected.layer(middleware::from_fn_with_state(
        state.config.clone(),
        auth::require_auth,
    ));

    Router::<AppState>::new()
        .route("/healthz", get(liveness))
        .route("/readyz", get(readyz))
        .merge(protected)
        .layer(TraceLayer::new_for_http())
        .layer(CorsLayer::permissive())
        .with_state(state)
}

async fn liveness() -> Json<Value> {
    Json(json!({ "status": "ok" }))
}

async fn readyz(State(state): State<AppState>) -> (StatusCode, Json<Value>) {
    match tokio::time::timeout(
        Duration::from_secs(2),
        sqlx::query("SELECT 1").execute(&state.pool),
    )
    .await
    {
        Ok(Ok(_)) => (StatusCode::OK, Json(json!({ "status": "ready" }))),
        _ => (
            StatusCode::SERVICE_UNAVAILABLE,
            Json(json!({ "status": "db not ready" })),
        ),
    }
}

#[cfg(test)]
mod tests {
    use tokio::{
        io::{AsyncReadExt, AsyncWriteExt},
        net::{TcpListener, TcpStream},
    };

    use super::router;
    use crate::{config::Config, state::AppState};

    #[tokio::test]
    async fn health_routes_work_without_a_database() {
        let config = Config {
            database_url: "postgres://127.0.0.1:1/test".to_owned(),
            api_token: "secret".to_owned(),
            port: 0,
            rust_log: String::new(),
        };
        let pool = sqlx::PgPool::connect_lazy(&config.database_url).unwrap();
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move {
            axum::serve(listener, router(AppState { pool, config }))
                .await
                .unwrap()
        });

        for (path, status, body) in [
            ("/healthz", "200 OK", "{\"status\":\"ok\"}"),
            (
                "/readyz",
                "503 Service Unavailable",
                "{\"status\":\"db not ready\"}",
            ),
        ] {
            let mut stream = TcpStream::connect(address).await.unwrap();
            stream
                .write_all(
                    format!("GET {path} HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                        .as_bytes(),
                )
                .await
                .unwrap();
            let mut response = Vec::new();
            stream.read_to_end(&mut response).await.unwrap();
            let response = String::from_utf8(response).unwrap();
            assert!(
                response.starts_with(&format!("HTTP/1.1 {status}")),
                "{response}"
            );
            assert!(response.contains(body), "{response}");
        }
        server.abort();
    }
}
