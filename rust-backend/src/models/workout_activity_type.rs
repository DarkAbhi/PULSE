use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Debug, Clone, Serialize, sqlx::FromRow)]
pub struct WorkoutActivityType {
    pub id: i32,
    pub raw_value: i32,
    pub name: String,
    pub created_at: DateTime<Utc>,
}
