use chrono::{DateTime, NaiveDate, Utc};
use serde::Serialize;
use sqlx::PgPool;

pub struct Rings {
    pub summary_date: NaiveDate,
    pub move_calories: f64,
    pub move_calories_goal: f64,
    pub exercise_minutes: i64,
    pub exercise_minutes_goal: i64,
    pub stand_hours: i64,
    pub stand_hours_goal: i64,
    pub steps_count: i64,
    pub time_zone: Option<String>,
}

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

pub async fn upsert(pool: &PgPool, user_id: i64, rings: &Rings) -> Result<(), sqlx::Error> {
    sqlx::query(
        "INSERT INTO fitness_activity_rings (user_id, summary_date, move_calories, move_calories_goal, \
         exercise_minutes, exercise_minutes_goal, stand_hours, stand_hours_goal, steps_count, updated_at) \
         VALUES ($1, $2, $3::double precision::numeric, $4::double precision::numeric, \
         $5, $6, $7, $8, $9, NOW()) \
         ON CONFLICT (user_id, summary_date) DO UPDATE SET \
         move_calories = EXCLUDED.move_calories, move_calories_goal = EXCLUDED.move_calories_goal, \
         exercise_minutes = EXCLUDED.exercise_minutes, exercise_minutes_goal = EXCLUDED.exercise_minutes_goal, \
         stand_hours = EXCLUDED.stand_hours, stand_hours_goal = EXCLUDED.stand_hours_goal, \
         steps_count = EXCLUDED.steps_count, updated_at = EXCLUDED.updated_at",
    )
    .bind(user_id)
    .bind(rings.summary_date)
    .bind(rings.move_calories)
    .bind(rings.move_calories_goal)
    .bind(rings.exercise_minutes)
    .bind(rings.exercise_minutes_goal)
    .bind(rings.stand_hours)
    .bind(rings.stand_hours_goal)
    .bind(rings.steps_count)
    .execute(pool)
    .await?;
    Ok(())
}

pub async fn time_zone_exists(pool: &PgPool, zone: &str) -> Result<bool, sqlx::Error> {
    sqlx::query_scalar("SELECT EXISTS (SELECT 1 FROM pg_timezone_names WHERE name = $1)")
        .bind(zone)
        .fetch_one(pool)
        .await
}
