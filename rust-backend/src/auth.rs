use axum::{
    extract::State,
    http::{Request, header},
    middleware::Next,
    response::Response,
};
use subtle::ConstantTimeEq;

use crate::{config::Config, error::AppError};

#[derive(Clone)]
pub struct AuthContext;

pub async fn require_auth(
    State(config): State<Config>,
    mut request: Request<axum::body::Body>,
    next: Next,
) -> Result<Response, AppError> {
    let token = request
        .headers()
        .get(header::AUTHORIZATION)
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.split_once(' '))
        .filter(|(scheme, token)| scheme.eq_ignore_ascii_case("Bearer") && !token.is_empty())
        .map(|(_, token)| token)
        .ok_or(AppError::Unauthorized)?;

    let context = verify_token(token, &config)?;
    request.extensions_mut().insert(context);
    Ok(next.run(request).await)
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
    use tokio::{
        io::{AsyncReadExt, AsyncWriteExt},
        net::{TcpListener, TcpStream},
    };

    use super::{AuthContext, require_auth};
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
}
