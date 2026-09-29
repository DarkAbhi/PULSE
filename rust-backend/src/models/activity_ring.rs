use chrono::{DateTime, NaiveDate, Utc};
use rust_decimal::Decimal;
use serde::Serialize;

#[derive(Debug, Clone, Serialize, sqlx::FromRow)]
pub struct ActivityRing {
    pub id: i64,
    pub summary_date: NaiveDate,
    pub move_calories: Decimal,
    pub move_calories_goal: Decimal,
    pub exercise_minutes: i32,
    pub exercise_minutes_goal: i32,
    pub stand_hours: i32,
    pub stand_hours_goal: i32,
    pub steps_count: i32,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}
