use sqlx::PgPool;

use crate::config::Config;

#[allow(dead_code)]
#[derive(Clone)]
pub struct AppState {
    pub pool: PgPool,
    pub config: Config,
}
