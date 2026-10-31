use std::time::Duration;

mod fitness_activity_rings;
mod fitness_input;
mod fitness_rings;

use axum::{
    Json, Router,
    extract::State,
    http::StatusCode,
    middleware,
    routing::{get, post},
};
use serde::Serialize;
use serde_json::{Value, json};
use tower_http::{cors::CorsLayer, trace::TraceLayer};

use crate::{
    auth,
    db::{activity_rings, workouts},
    error::AppError,
    metrics::{self, Metrics},
    state::AppState,
};

#[derive(Serialize)]
struct FitnessData {
    snapshot: Option<activity_rings::FitnessSnapshot>,
    workouts: Vec<workouts::FitnessWorkout>,
}

pub fn router(state: AppState, metrics: std::sync::Arc<Metrics>) -> Router {
    let fitness_writes = Router::<AppState>::new()
        .route("/api/shortcut/fitness-rings", post(fitness_rings::create))
        .route(
            "/api/fitness-activity-rings",
            post(fitness_activity_rings::create),
        )
        .layer(middleware::from_fn_with_state(
            state.clone(),
            auth::require_api_key,
        ));

    Router::<AppState>::new()
        .route("/healthz", get(liveness))
        .route("/api/version", get(pulse_version))
        .route("/readyz", get(readyz))
        .route("/api/fitness-summary", get(fitness_summary))
        .merge(fitness_writes)
        .layer(TraceLayer::new_for_http())
        .layer(CorsLayer::permissive())
        .layer(middleware::from_fn_with_state(metrics, metrics::observe))
        .with_state(state)
}

async fn pulse_version() -> Json<Value> {
    Json(
        serde_json::from_str(include_str!("../../../release.json"))
            .expect("release.json must contain valid JSON"),
    )
}

async fn liveness() -> Json<Value> {
    Json(json!({ "status": "ok" }))
}

async fn fitness_summary(State(state): State<AppState>) -> Result<Json<FitnessData>, AppError> {
    let (snapshot, workouts) = tokio::try_join!(
        activity_rings::latest(&state.pool),
        workouts::latest_relevant(&state.pool),
    )?;
    Ok(Json(FitnessData { snapshot, workouts }))
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
    use crate::state::AppState;

    #[tokio::test]
    async fn health_routes_work_without_a_database() {
        let pool = sqlx::postgres::PgPoolOptions::new()
            .acquire_timeout(std::time::Duration::from_millis(100))
            .connect_lazy("postgres://127.0.0.1:1/test")
            .unwrap();
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move {
            axum::serve(
                listener,
                router(AppState { pool }, crate::metrics::Metrics::new().unwrap()),
            )
            .await
            .unwrap();
        });

        let release: serde_json::Value =
            serde_json::from_str(include_str!("../../../release.json")).unwrap();
        let release_body = release.to_string();
        for (path, status, body) in [
            ("/api/version", "200 OK", release_body.as_str()),
            ("/healthz", "200 OK", "{\"status\":\"ok\"}"),
            (
                "/readyz",
                "503 Service Unavailable",
                "{\"status\":\"db not ready\"}",
            ),
            (
                "/api/fitness-summary",
                "500 Internal Server Error",
                "{\"error\":\"internal server error\"}",
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
    #[tokio::test]
    async fn fitness_routes_use_key_ownership_and_real_migration_bounds() {
        use serde_json::json;
        use sha2::{Digest, Sha256};

        let database = crate::testhelper::TestDatabase::start().await;
        let pool = database.pool.clone();
        for migration in [
            include_str!("../../../go-backend/migrations/00001_init.up.sql"),
            include_str!("../../../go-backend/migrations/00017_fitness_workouts.up.sql"),
            include_str!("../../../go-backend/migrations/00018_api_keys.up.sql"),
            include_str!("../../../go-backend/migrations/00019_fitness_user_id.up.sql"),
            include_str!("../../../go-backend/migrations/00020_vehicle_gym_ownership.up.sql"),
            include_str!(
                "../../../go-backend/migrations/00021_backfill_gym_visits_from_workouts.up.sql"
            ),
        ] {
            sqlx::raw_sql(migration).execute(&pool).await.unwrap();
        }
        let owner: i64 = sqlx::query_scalar("INSERT INTO users (username, password_hash) VALUES ('key-owner', 'unused') RETURNING id")
            .fetch_one(&pool).await.unwrap();
        let hash = format!("{:x}", Sha256::digest(b"lt_route_test"));
        sqlx::query("INSERT INTO api_keys (user_id, name, token_hash, token_prefix) VALUES ($1, 'test', $2, 'lt_route')")
            .bind(owner).bind(hash).execute(&pool).await.unwrap();
        let app = router(
            AppState { pool: pool.clone() },
            crate::metrics::Metrics::new().unwrap(),
        );
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move {
            axum::serve(listener, app).await.unwrap();
        });

        let flat = json!({"summary_date": "2026-07-27", "time_zone": "Asia/Kolkata",
            "move": "999999.99", "move_goal": 999_999.99, "exercise ": 2_147_483_647.0,
            "exercise_goal": "2147483647", "stand": 2_147_483_647, "stand_goal": 2_147_483_647,
            "step_count": "2147483647"});
        let nested = json!({"summary_date": "2026-07-27", "time_zone": "Asia/Kolkata", "captured_at": "2026-07-27T00:00:00Z",
            "move": {"value": 999_999.99, "goal": 999_999.99, "unit": "kcal"},
            "exercise": {"value": 2_147_483_647.0, "goal": 2_147_483_647, "unit": "min"},
            "stand": {"value": 2_147_483_647, "goal": 2_147_483_647, "unit": "hr"}, "step_count": 2_147_483_647,
            "workouts": [{"uuid": "6b29fc40-ca47-1000-8000-00805f9b34fb", "activity_type_raw": 50,
                "activity_type_name": "Traditional Strength Training", "start_date": "2026-07-27T00:00:00Z",
                "end_date": "2026-07-27T00:01:00Z", "duration_seconds": 9_999_999.999,
                "calories_burned_kcal": 999_999.99, "distance_meters": 99_999_999.99}]});
        for (path, payload) in [
            ("/api/shortcut/fitness-rings", flat),
            ("/api/fitness-activity-rings", nested),
        ] {
            let body = payload.to_string();
            for (authorization, status) in [
                ("", "401"),
                ("Authorization: Basic key\r\n", "401"),
                ("Authorization: Bearer wrong\r\n", "401"),
                ("Authorization: bEaReR lt_route_test\r\n", "201"),
            ] {
                let mut stream = TcpStream::connect(address).await.unwrap();
                stream.write_all(format!("POST {path} HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n{authorization}Content-Length: {}\r\nConnection: close\r\n\r\n{body}", body.len()).as_bytes()).await.unwrap();
                let mut response = Vec::new();
                stream.read_to_end(&mut response).await.unwrap();
                let response = String::from_utf8(response).unwrap();
                assert!(
                    response.starts_with(&format!("HTTP/1.1 {status}")),
                    "{path}: {response}"
                );
                if status == "401" {
                    assert!(
                        response
                            .to_ascii_lowercase()
                            .contains("www-authenticate: bearer"),
                        "{response}"
                    );
                }
            }
            let saved: (i64, i32) =
                sqlx::query_as("SELECT user_id, steps_count FROM fitness_activity_rings")
                    .fetch_one(&pool)
                    .await
                    .unwrap();
            assert_eq!(saved, (owner, i32::MAX), "{path} must use the key owner");
        }
        let workout_owner: i64 = sqlx::query_scalar("SELECT user_id FROM fitness_workouts")
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(workout_owner, owner);
        sqlx::query("UPDATE api_keys SET revoked_at = NOW()")
            .execute(&pool)
            .await
            .unwrap();
        let mut stream = TcpStream::connect(address).await.unwrap();
        stream.write_all(b"POST /api/shortcut/fitness-rings HTTP/1.1\r\nHost: localhost\r\nAuthorization: Bearer lt_route_test\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}").await.unwrap();
        let mut response = Vec::new();
        stream.read_to_end(&mut response).await.unwrap();
        assert!(
            String::from_utf8(response)
                .unwrap()
                .starts_with("HTTP/1.1 401")
        );
        server.abort();
    }
}
