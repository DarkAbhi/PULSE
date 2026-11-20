use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use sqlx::{PgPool, PgTransaction, types::Json};

#[derive(Deserialize)]
pub struct Sleep {
    pub sleep_date: NaiveDate,
    total_asleep_seconds: i32,
    main_sleep_seconds: i32,
    sessions: Vec<Session>,
}

#[derive(Deserialize, Serialize)]
struct Session {
    session_id: String,
    start: DateTime<Utc>,
    end: DateTime<Utc>,
    asleep_seconds: i32,
    is_main_sleep: bool,
}

impl Sleep {
    pub fn validate(&self, summary_date: NaiveDate) -> Result<(), &'static str> {
        if self.sleep_date != summary_date {
            return Err("sleep.sleep_date must match summary_date.");
        }
        if !(0..=86_400).contains(&self.total_asleep_seconds) {
            return Err("sleep.total_asleep_seconds must be between 0 and 86400.");
        }
        if self.sessions.is_empty() {
            return Err("sleep.sessions must contain at least one session.");
        }
        if self.sessions.iter().any(|session| {
            session.start >= session.end
                || session.asleep_seconds < 0
                || session.session_id.is_empty()
        }) {
            return Err(
                "Each sleep session needs a session_id, start before end, and non-negative asleep_seconds.",
            );
        }
        let total: i64 = self
            .sessions
            .iter()
            .map(|s| i64::from(s.asleep_seconds))
            .sum();
        if total != i64::from(self.total_asleep_seconds) {
            return Err("sleep.total_asleep_seconds must equal the sum of the sessions.");
        }
        let mut main = self.sessions.iter().filter(|s| s.is_main_sleep);
        if main.next().map(|s| s.asleep_seconds) != Some(self.main_sleep_seconds)
            || main.next().is_some()
            || self
                .sessions
                .iter()
                .any(|s| s.asleep_seconds > self.main_sleep_seconds)
        {
            return Err(
                "The largest sleep session must be the only main session and match sleep.main_sleep_seconds.",
            );
        }
        Ok(())
    }
}

pub async fn upsert(
    tx: &mut PgTransaction<'_>,
    user_id: i64,
    sleep: &Sleep,
) -> Result<(), sqlx::Error> {
    sqlx::query(
        "INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions) \
         VALUES ($1, $2, $3, $4, $5, $6) \
         ON CONFLICT (user_id, sleep_date) DO UPDATE SET \
         total_asleep_seconds = EXCLUDED.total_asleep_seconds, main_sleep_seconds = EXCLUDED.main_sleep_seconds, \
         wake_at = EXCLUDED.wake_at, sessions = EXCLUDED.sessions",
    )
    .bind(user_id)
    .bind(sleep.sleep_date)
    .bind(sleep.total_asleep_seconds)
    .bind(sleep.main_sleep_seconds)
    .bind(sleep.sessions.iter().map(|s| s.end).max())
    .bind(Json(&sleep.sessions))
    .execute(&mut **tx)
    .await?;
    Ok(())
}

pub async fn latest(pool: &PgPool, user_id: i64) -> Result<(NaiveDate, i32, bool), sqlx::Error> {
    sqlx::query_as(
        "SELECT sleep_date, total_asleep_seconds / 60, wake_at < NOW() - INTERVAL '48 hours' \
         FROM fitness_sleep WHERE user_id = $1 ORDER BY sleep_date DESC LIMIT 1",
    )
    .bind(user_id)
    .fetch_one(pool)
    .await
}
