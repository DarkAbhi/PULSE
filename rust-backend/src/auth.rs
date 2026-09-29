use axum::{
    extract::State,
    http::{Request, header},
    middleware::Next,
    response::Response,
};
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use subtle::ConstantTimeEq;

use crate::{config::Config, error::AppError, state::AppState};

#[derive(Clone)]
pub struct AuthContext;

#[derive(Clone)]
pub struct ApiKeyUser(pub i64);

pub async fn require_api_key(
    State(state): State<AppState>,
    mut request: Request<axum::body::Body>,
    next: Next,
) -> Result<Response, AppError> {
    let token = bearer_token(&request)?;
    let user_id = api_key_user(&state.pool, token)
        .await?
        .ok_or(AppError::Unauthorized)?;
    request.extensions_mut().insert(ApiKeyUser(user_id));
    Ok(next.run(request).await)
}

async fn api_key_user(pool: &PgPool, token: &str) -> Result<Option<i64>, sqlx::Error> {
    let hash = format!("{:x}", Sha256::digest(token.as_bytes()));
    sqlx::query_scalar("SELECT user_id FROM api_keys WHERE token_hash = $1 AND revoked_at IS NULL")
        .bind(hash)
        .fetch_optional(pool)
        .await
}

pub async fn require_auth(
    State(config): State<Config>,
    mut request: Request<axum::body::Body>,
    next: Next,
) -> Result<Response, AppError> {
    let token = bearer_token(&request)?;
    let context = verify_token(token, &config)?;
    request.extensions_mut().insert(context);
    Ok(next.run(request).await)
}

fn bearer_token(request: &Request<axum::body::Body>) -> Result<&str, AppError> {
    request
        .headers()
        .get(header::AUTHORIZATION)
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.split_once(' '))
        .filter(|(scheme, token)| scheme.eq_ignore_ascii_case("Bearer") && !token.is_empty())
        .map(|(_, token)| token)
        .ok_or(AppError::Unauthorized)
}

fn verify_token(token: &str, config: &Config) -> Result<AuthContext, AppError> {
    if bool::from(token.as_bytes().ct_eq(config.api_token.as_bytes())) {
        Ok(AuthContext)
    } else {
        Err(AppError::Unauthorized)
    }
}

#[cfg(test)]
mod tests {
    use axum::{Extension, Router, http::StatusCode, middleware, routing::get};
    use sha2::Digest;
    use tokio::{
        io::{AsyncReadExt, AsyncWriteExt},
        net::{TcpListener, TcpStream},
    };

    use super::{AuthContext, api_key_user, require_auth};
    use crate::config::Config;

    #[tokio::test]
    async fn bearer_middleware() {
        let config = Config {
            database_url: String::new(),
            api_token: "secret".to_owned(),
            port: 0,
            rust_log: String::new(),
        };
        let app = Router::new()
            .route(
                "/test",
                get(|Extension(_): Extension<AuthContext>| async { StatusCode::NO_CONTENT }),
            )
            .layer(middleware::from_fn_with_state(config, require_auth));
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let server = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });

        for (header, expected_status) in [
            ("", "401 Unauthorized"),
            ("Authorization: Basic secret\r\n", "401 Unauthorized"),
            ("Authorization: Bearer wrong\r\n", "401 Unauthorized"),
            ("Authorization: Bearer secret\r\n", "204 No Content"),
        ] {
            let mut stream = TcpStream::connect(address).await.unwrap();
            stream
                .write_all(
                    format!(
                        "GET /test HTTP/1.1\r\nHost: localhost\r\n{header}Connection: close\r\n\r\n"
                    )
                    .as_bytes(),
                )
                .await
                .unwrap();
            let mut response = Vec::new();
            stream.read_to_end(&mut response).await.unwrap();
            let response = String::from_utf8(response).unwrap();
            assert!(
                response.starts_with(&format!("HTTP/1.1 {expected_status}")),
                "{response}"
            );
            if expected_status == "401 Unauthorized" {
                assert!(
                    response
                        .to_ascii_lowercase()
                        .contains("www-authenticate: bearer")
                );
                assert!(response.contains("{\"error\":\"unauthorized\"}"));
            }
        }

        server.abort();
    }

    #[tokio::test]
    async fn api_key_maps_to_its_owner_and_revocation_takes_effect() {
        let Ok(url) = std::env::var("TEST_DATABASE_URL") else {
            return;
        };
        let pool = sqlx::postgres::PgPoolOptions::new()
            .max_connections(1)
            .connect(&url)
            .await
            .unwrap();
        sqlx::query("CREATE TEMP TABLE api_keys (user_id bigint, token_hash char(64), revoked_at timestamptz)")
            .execute(&pool)
            .await
            .unwrap();
        let token = "lt_test_key";
        let hash = format!("{:x}", sha2::Sha256::digest(token.as_bytes()));
        sqlx::query("INSERT INTO api_keys (user_id, token_hash) VALUES ($1, $2)")
            .bind(42_i64)
            .bind(hash)
            .execute(&pool)
            .await
            .unwrap();
        assert_eq!(api_key_user(&pool, token).await.unwrap(), Some(42));
        assert_eq!(api_key_user(&pool, "wrong").await.unwrap(), None);
        sqlx::query("UPDATE api_keys SET revoked_at = NOW()")
            .execute(&pool)
            .await
            .unwrap();
        assert_eq!(api_key_user(&pool, token).await.unwrap(), None);
    }
}
