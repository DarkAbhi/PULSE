mod auth;
mod config;
mod error;
mod metrics;
mod routes;
mod state;

mod db;

#[cfg(test)]
mod testhelper;

use std::{error::Error, io, net::SocketAddr};

use sqlx::PgPool;
use tokio::net::TcpListener;
use tracing_subscriber::EnvFilter;

use crate::{config::Config, state::AppState};

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    dotenvy::dotenv().ok();
    let config = Config::from_env()?;
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_new(&config.rust_log)?)
        .init();

    let pool = PgPool::connect(&config.database_url)
        .await
        .map_err(|error| io::Error::other(format!("database connection failed: {error}")))?;
    sqlx::query_scalar::<_, i32>("SELECT 1")
        .fetch_one(&pool)
        .await
        .map_err(|error| {
            io::Error::other(format!("database connectivity check failed: {error}"))
        })?;

    let address = SocketAddr::from(([0, 0, 0, 0], config.port));
    let metrics = metrics::Metrics::new()?;
    let metrics_app = metrics.clone().router(pool.clone());
    let app = routes::router(AppState { pool }, metrics);
    let listener = TcpListener::bind(address).await?;
    // Keep metrics off the API port exposed through Cloudflare.
    let metrics_listener = TcpListener::bind("0.0.0.0:9091").await?;
    tracing::info!(%address, "listening");
    tokio::try_join!(
        async {
            axum::serve(listener, app)
                .with_graceful_shutdown(shutdown_signal())
                .await
        },
        async {
            axum::serve(metrics_listener, metrics_app)
                .with_graceful_shutdown(shutdown_signal())
                .await
        },
    )?;
    Ok(())
}

async fn shutdown_signal() {
    let ctrl_c = async {
        tokio::signal::ctrl_c()
            .await
            .expect("failed to listen for SIGINT")
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("failed to listen for SIGTERM")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        () = ctrl_c => {},
        () = terminate => {},
    }
}
