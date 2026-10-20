use axum::{
    Json,
    http::{HeaderValue, StatusCode, header},
    response::{IntoResponse, Response},
};
use serde_json::json;

#[allow(dead_code)]
#[derive(Debug, thiserror::Error)]
pub enum AppError {
    #[error("unauthorized")]
    Unauthorized,
    #[error(transparent)]
    Database(#[from] sqlx::Error),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            Self::Unauthorized => (StatusCode::UNAUTHORIZED, "unauthorized"),
            Self::Database(sqlx::Error::RowNotFound) => (StatusCode::NOT_FOUND, "not found"),
            Self::Database(error) => {
                tracing::error!(%error, "database request failed");
                (StatusCode::INTERNAL_SERVER_ERROR, "internal server error")
            }
        };
        let mut response = (status, Json(json!({ "error": message }))).into_response();
        if status == StatusCode::UNAUTHORIZED {
            response
                .headers_mut()
                .insert(header::WWW_AUTHENTICATE, HeaderValue::from_static("Bearer"));
        }
        response
    }
}
