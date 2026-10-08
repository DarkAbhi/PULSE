use axum::{
    extract::State,
    http::{Request, header},
    middleware::Next,
    response::Response,
};
use sha2::{Digest, Sha256};
use sqlx::PgPool;

use crate::{error::AppError, state::AppState};

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

#[cfg(test)]
mod tests {
    use axum::{body::Body, http::Request};
    use sha2::Digest;

    use super::{api_key_user, bearer_token};

    #[test]
    fn bearer_header_requires_a_token() {
        for (header, expected) in [
            (None, None),
            (Some("Basic key"), None),
            (Some("Bearer "), None),
            (Some("Bearer key"), Some("key")),
        ] {
            let mut request = Request::new(Body::empty());
            if let Some(header) = header {
                request
                    .headers_mut()
                    .insert(axum::http::header::AUTHORIZATION, header.parse().unwrap());
            }
            assert_eq!(bearer_token(&request).ok(), expected);
        }
    }

    #[tokio::test]
    async fn api_key_maps_to_its_owner_and_revocation_takes_effect() {
        let database = crate::testhelper::TestDatabase::start().await;
        let pool = database.pool.clone();
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
