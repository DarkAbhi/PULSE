mod auth;
mod config;
mod error;
mod routes;
mod state;

#[allow(dead_code)]
mod db;
#[allow(dead_code)]
mod models;

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
    let app = routes::router(AppState { pool, config });
    let listener = TcpListener::bind(address).await?;
    tracing::info!(%address, "listening");
    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await?;
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
