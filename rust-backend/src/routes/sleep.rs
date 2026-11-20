use axum::{Extension, Json, extract::State, http::header};
use serde_json::{Value, json};

use crate::{auth::ApiKeyUser, db::sleep, error::AppError, state::AppState};

pub async fn latest(
    State(state): State<AppState>,
    Extension(ApiKeyUser(user_id)): Extension<ApiKeyUser>,
) -> Result<([(header::HeaderName, &'static str); 2], Json<Value>), AppError> {
    let (sleep_date, total_minutes, stale) = sleep::latest(&state.pool, user_id).await?;
    let mut body = json!({
        "sleep_date": sleep_date,
        "total_minutes": total_minutes,
        "display": format!("{}h {}m", total_minutes / 60, total_minutes % 60),
    });
    if stale {
        body["stale"] = json!(true);
    }
    Ok((
        [
            (header::CACHE_CONTROL, "private, max-age=300"),
            (header::VARY, "Authorization"),
        ],
        Json(body),
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn sleep_requires_a_key_and_returns_only_the_owners_latest_total() {
        use sha2::{Digest, Sha256};
        use tokio::{
            io::{AsyncReadExt, AsyncWriteExt},
            net::{TcpListener, TcpStream},
        };

        let database = crate::testhelper::TestDatabase::start().await;
        let pool = &database.pool;
        sqlx::raw_sql(
            "CREATE TABLE users (id BIGINT PRIMARY KEY); INSERT INTO users VALUES (1), (2);",
        )
        .execute(pool)
        .await
        .unwrap();
        sqlx::raw_sql(include_str!(
            "../../../go-backend/migrations/00043_fitness_sleep.up.sql"
        ))
        .execute(pool)
        .await
        .unwrap();
        sqlx::raw_sql(include_str!(
            "../../../go-backend/migrations/00018_api_keys.up.sql"
        ))
        .execute(pool)
        .await
        .unwrap();
        for (user_id, token) in [(1_i64, "lt_sleep_owner_one"), (2, "lt_sleep_owner_two")] {
            sqlx::query("INSERT INTO api_keys (user_id, name, token_hash, token_prefix) VALUES ($1, 'test', $2, 'lt_sleep')")
                .bind(user_id)
                .bind(hex::encode(Sha256::digest(token.as_bytes())))
                .execute(pool).await.unwrap();
        }
        sqlx::query(
            "INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions) VALUES              (1, '2026-07-25', 7200, 7200, NOW(), '[{}]'),              (1, '2026-07-26', 27001, 25200, NOW(), '[{}]'),              (2, '2026-07-27', 3600, 3600, NOW(), '[{}]')",
        ).execute(pool).await.unwrap();

        let app = super::super::router(
            AppState { pool: pool.clone() },
            crate::metrics::Metrics::new().unwrap(),
        );
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });
        let get = async |authorization: &str| {
            let mut stream = TcpStream::connect(address).await.unwrap();
            stream.write_all(format!(
                "GET /api/health/latest HTTP/1.1\r\nHost: localhost\r\n{authorization}Connection: close\r\n\r\n",
            ).as_bytes()).await.unwrap();
            let mut response = String::new();
            stream.read_to_string(&mut response).await.unwrap();
            response
        };
        for authorization in ["", "Authorization: Bearer invalid\r\n"] {
            assert!(get(authorization).await.starts_with("HTTP/1.1 401"));
        }

        let get_sleep = async |authorization| {
            let response = get(authorization).await;
            assert!(response.starts_with("HTTP/1.1 200"), "{response}");
            let (headers, body) = response.split_once("\r\n\r\n").unwrap();
            assert!(headers.contains("cache-control: private, max-age=300"));
            assert!(
                headers
                    .lines()
                    .any(|line| line.to_ascii_lowercase().starts_with("vary: authorization"))
            );
            serde_json::from_str::<Value>(body).unwrap()
        };
        let owner_one = "Authorization: Bearer lt_sleep_owner_one\r\n";
        let owner_two = "Authorization: Bearer lt_sleep_owner_two\r\n";
        let mut expected =
            json!({"sleep_date": "2026-07-26", "total_minutes": 450, "display": "7h 30m"});
        assert_eq!(get_sleep(owner_one).await, expected);
        assert_eq!(
            get_sleep(owner_two).await,
            json!({"sleep_date": "2026-07-27", "total_minutes": 60, "display": "1h 0m"})
        );

        sqlx::query(
            "UPDATE fitness_sleep SET wake_at = NOW() - INTERVAL '49 hours' WHERE user_id = 1",
        )
        .execute(pool)
        .await
        .unwrap();
        expected["stale"] = json!(true);
        assert_eq!(get_sleep(owner_one).await, expected);
        server.abort();
    }
}
