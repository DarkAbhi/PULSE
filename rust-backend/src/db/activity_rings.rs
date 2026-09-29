use chrono::{DateTime, NaiveDate, Utc};
use serde::Serialize;
use sqlx::PgPool;

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct FitnessSnapshot {
    pub summary_date: NaiveDate,
    pub updated_at: DateTime<Utc>,
    pub move_calories: f64,
    pub move_calories_goal: f64,
    pub exercise_minutes: i32,
    pub exercise_minutes_goal: i32,
    pub stand_hours: i32,
    pub stand_hours_goal: i32,
    pub steps_count: i32,
}

pub async fn latest(pool: &PgPool) -> Result<Option<FitnessSnapshot>, sqlx::Error> {
    sqlx::query_as(
        "SELECT summary_date, updated_at, move_calories::double precision AS move_calories, \
         move_calories_goal::double precision AS move_calories_goal, exercise_minutes, \
         exercise_minutes_goal, stand_hours, stand_hours_goal, steps_count \
         FROM fitness_activity_rings ORDER BY summary_date DESC LIMIT 1",
    )
    .fetch_optional(pool)
    .await
}
