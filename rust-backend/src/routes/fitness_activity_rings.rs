use axum::{Extension, Json, body::Bytes, extract::State, http::StatusCode};
use chrono::{DateTime, NaiveDate, Utc};
use serde::Serialize;
use serde_json::{Value, json};
use sqlx::Postgres;
use std::collections::HashMap;
use uuid::Uuid;

use crate::{auth::ApiKeyUser, state::AppState};

type Reply = (StatusCode, Json<Value>);

#[derive(sqlx::FromRow, Serialize)]
struct SavedRings {
    id: i64,
    summary_date: NaiveDate,
    move_calories: f64,
    move_calories_goal: f64,
    exercise_minutes: i32,
    exercise_minutes_goal: i32,
    stand_hours: i32,
    stand_hours_goal: i32,
    steps_count: i32,
    created_at: DateTime<Utc>,
    updated_at: DateTime<Utc>,
}

#[derive(sqlx::FromRow, Serialize)]
struct SavedWorkout {
    id: i64,
    uuid: Uuid,
}

fn reply(status: StatusCode, body: Value) -> Reply {
    (status, Json(body))
}

fn invalid(details: String) -> Reply {
    reply(
        StatusCode::BAD_REQUEST,
        json!({
            "error": "The activity-rings payload is invalid.", "details": details
        }),
    )
}

fn number(value: Option<&Value>) -> Option<f64> {
    value?.as_f64().filter(|n| n.is_finite() && *n >= 0.0)
}

fn integer(value: Option<&Value>) -> Option<i64> {
    let n = value?.as_f64()?;
    (n.is_finite() && n >= 0.0 && n.fract() == 0.0 && n < i64::MAX as f64).then_some(n as i64)
}

fn date_time(value: Option<&Value>) -> Option<DateTime<Utc>> {
    DateTime::parse_from_rfc3339(value?.as_str()?)
        .ok()
        .map(|date| date.with_timezone(&Utc))
}

fn metric(value: Option<&Value>, unit: &str, whole: bool) -> Option<(f64, f64)> {
    let value = value?;
    if value.get("unit")?.as_str()? != unit {
        return None;
    }
    if whole {
        Some((
            integer(value.get("value"))? as f64,
            integer(value.get("goal"))? as f64,
        ))
    } else {
        Some((number(value.get("value"))?, number(value.get("goal"))?))
    }
}

fn workout_error(value: &Value) -> Option<&'static str> {
    let Some(workout) = value.as_object() else {
        return Some("must be an object.");
    };
    let valid_uuid = workout
        .get("uuid")
        .and_then(Value::as_str)
        .is_some_and(|uuid| {
            uuid.len() == 36
                && uuid.bytes().enumerate().all(|(i, byte)| {
                    if [8, 13, 18, 23].contains(&i) {
                        byte == b'-'
                    } else {
                        byte.is_ascii_hexdigit()
                    }
                })
        });
    if !valid_uuid {
        return Some("uuid must be a UUID string.");
    }
    if integer(workout.get("activity_type_raw")).is_none() {
        return Some("activity_type_raw must be a non-negative integer.");
    }
    if !workout
        .get("activity_type_name")
        .and_then(Value::as_str)
        .is_some_and(|name| !name.trim().is_empty() && name.encode_utf16().count() <= 100)
    {
        return Some("activity_type_name must be a non-empty string of up to 100 characters.");
    }
    let start = date_time(workout.get("start_date"));
    let end = date_time(workout.get("end_date"));
    if !matches!((start, end), (Some(start), Some(end)) if end >= start) {
        return Some(
            "start_date and end_date must be valid ISO date-times, with end_date after start_date.",
        );
    }
    if number(workout.get("duration_seconds")).is_none() {
        return Some("duration_seconds must be a non-negative number.");
    }
    for (key, message) in [
        (
            "calories_burned_kcal",
            "calories_burned_kcal must be a non-negative number or null.",
        ),
        (
            "distance_meters",
            "distance_meters must be a non-negative number or null.",
        ),
    ] {
        if workout
            .get(key)
            .is_some_and(|value| !value.is_null() && number(Some(value)).is_none())
        {
            return Some(message);
        }
    }
    if workout
        .get("metadata")
        .is_some_and(|value| !value.is_null() && !value.is_object())
    {
        return Some("metadata must be an object, null, or omitted.");
    }
    None
}

fn validate_header(payload: &Value) -> Result<(), String> {
    if !payload.is_object() && !payload.is_array() {
        return Err("Request body must be a JSON object.".into());
    }
    if date_time(payload.get("captured_at")).is_none() {
        return Err("captured_at must be a valid ISO date-time.".into());
    }
    if !payload
        .get("summary_date")
        .and_then(Value::as_str)
        .is_some_and(|date| {
            date.len() == 10
                && date.as_bytes()[4] == b'-'
                && date.as_bytes()[7] == b'-'
                && NaiveDate::parse_from_str(date, "%Y-%m-%d").is_ok()
        })
    {
        return Err("summary_date must be a valid YYYY-MM-DD date.".into());
    }
    if let Some(zone) = payload.get("time_zone")
        && !zone.is_string()
    {
        return Err("time_zone must be a valid IANA time zone.".into());
    }
    Ok(())
}

fn validate_body(payload: &Value) -> Result<(), String> {
    for (key, unit, whole, message) in [
        (
            "move",
            "kcal",
            false,
            "move must include non-negative value and goal numbers with unit 'kcal'.",
        ),
        (
            "exercise",
            "min",
            true,
            "exercise must include non-negative integer value and goal values with unit 'min'.",
        ),
        (
            "stand",
            "hr",
            true,
            "stand must include non-negative integer value and goal values with unit 'hr'.",
        ),
    ] {
        if metric(payload.get(key), unit, whole).is_none() {
            return Err(message.into());
        }
    }
    if integer(payload.get("step_count")).is_none() {
        return Err("step_count is required and must be a non-negative integer.".into());
    }
    if let Some(workouts) = payload.get("workouts") {
        let Some(workouts) = workouts.as_array() else {
            return Err("workouts must be an array.".into());
        };
        for (index, workout) in workouts.iter().enumerate() {
            if let Some(message) = workout_error(workout) {
                return Err(format!("workouts[{index}] {message}"));
            }
        }
    }
    Ok(())
}

#[cfg(test)]
fn validate(payload: &Value) -> Result<(), String> {
    validate_header(payload)?;
    validate_body(payload)
}

pub async fn create(
    State(state): State<AppState>,
    Extension(user): Extension<ApiKeyUser>,
    body: Bytes,
) -> Reply {
    let Ok(payload) = serde_json::from_slice::<Value>(&body) else {
        return reply(
            StatusCode::BAD_REQUEST,
            json!({"error": "Request body must be valid JSON."}),
        );
    };
    if let Err(details) = validate_header(&payload) {
        return invalid(details);
    }
    if let Some(zone) = payload.get("time_zone").and_then(Value::as_str) {
        match sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS (SELECT 1 FROM pg_timezone_names WHERE name = $1)",
        )
        .bind(zone)
        .fetch_one(&state.pool)
        .await
        {
            Ok(true) => {}
            Ok(false) => return invalid("time_zone must be a valid IANA time zone.".into()),
            Err(error) => {
                tracing::error!(%error, "unable to validate time zone");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save activity rings."}),
                );
            }
        }
    }
    if let Err(details) = validate_body(&payload) {
        return invalid(details);
    }
    let workouts = payload.get("workouts").and_then(Value::as_array);
    let mut activity_types = HashMap::new();
    for workout in workouts.into_iter().flatten() {
        activity_types.insert(
            integer(workout.get("activity_type_raw")).unwrap(),
            workout["activity_type_name"].as_str().unwrap().trim(),
        );
    }
    let mut activity_type_ids = HashMap::new();
    let mut tx = match state.pool.begin().await {
        Ok(tx) => tx,
        Err(error) => {
            tracing::error!(%error, "unable to save workout activity types");
            return reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to save workout activity types."}),
            );
        }
    };
    for (raw, name) in activity_types {
        let saved = sqlx::query_scalar::<_, i32>(
            "INSERT INTO workout_activity_types (raw_value, name) VALUES ($1::bigint, $2) \
             ON CONFLICT (raw_value) DO UPDATE SET name = EXCLUDED.name RETURNING id",
        )
        .bind(raw)
        .bind(name)
        .fetch_one(&mut *tx)
        .await;
        match saved {
            Ok(id) => {
                activity_type_ids.insert(raw, id);
            }
            Err(error) => {
                tracing::error!(%error, "unable to save workout activity types");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save workout activity types."}),
                );
            }
        }
    }
    if workouts
        .into_iter()
        .flatten()
        .any(|workout| integer(workout.get("activity_type_raw")) == Some(50))
    {
        // ponytail: one lock per user; use per-day locks if uploads become contentious.
        if let Err(error) = sqlx::query("SELECT pg_advisory_xact_lock($1)")
            .bind(user.0)
            .execute(&mut *tx)
            .await
        {
            tracing::error!(%error, "unable to lock gym visits");
            return reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to save gym visit."}),
            );
        }
    }
    let mut saved_workouts = Vec::new();
    let mut workout_uuids = std::collections::HashSet::new();
    for workout in workouts.into_iter().flatten() {
        if !workout_uuids.insert(workout["uuid"].as_str().unwrap().to_ascii_lowercase()) {
            return reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to save workouts."}),
            );
        }
        let raw = integer(workout.get("activity_type_raw")).unwrap();
        let Some(&activity_type_id) = activity_type_ids.get(&raw) else {
            return reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to match workout activity types."}),
            );
        };
        let metadata = workout
            .get("metadata")
            .filter(|value| !value.is_null())
            .cloned()
            .unwrap_or_else(|| json!({}));
        let saved = sqlx::query_as::<_, SavedWorkout>(
            "INSERT INTO fitness_workouts (user_id, uuid, activity_type_id, start_time, end_time, \
             duration_seconds, calories_burned, distance_meters, metadata, updated_at) \
             VALUES ($1, $2, $3, $4, $5, $6::double precision::numeric, $7::double precision::numeric, \
             $8::double precision::numeric, $9, NOW()) \
             ON CONFLICT (user_id, uuid) DO UPDATE SET activity_type_id = EXCLUDED.activity_type_id, \
             start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time, \
             duration_seconds = EXCLUDED.duration_seconds, calories_burned = EXCLUDED.calories_burned, \
             distance_meters = EXCLUDED.distance_meters, metadata = EXCLUDED.metadata, \
             updated_at = EXCLUDED.updated_at RETURNING id, uuid"
        )
        .bind(user.0)
        .bind(Uuid::parse_str(workout["uuid"].as_str().unwrap()).unwrap())
        .bind(activity_type_id)
        .bind(date_time(workout.get("start_date")).unwrap())
        .bind(date_time(workout.get("end_date")).unwrap())
        .bind(number(workout.get("duration_seconds")).unwrap())
        .bind(number(workout.get("calories_burned_kcal")))
        .bind(number(workout.get("distance_meters")))
        .bind(sqlx::types::Json(metadata))
        .fetch_one(&mut *tx).await;
        match saved {
            Ok(row) => {
                if raw == 50 {
                    let start = date_time(workout.get("start_date")).unwrap();
                    let visit = sqlx::query(
                        "INSERT INTO gym_visits (user_id, fitness_workout_id, created_at, updated_at) \
                         SELECT $1, $2, $3, $3 WHERE NOT EXISTS ( \
                             SELECT 1 FROM gym_visits WHERE user_id = $1 \
                             AND (created_at AT TIME ZONE 'Asia/Kolkata')::date = \
                                 ($3::timestamptz AT TIME ZONE 'Asia/Kolkata')::date)",
                    )
                    .bind(user.0)
                    .bind(row.id)
                    .bind(start)
                    .execute(&mut *tx)
                    .await;
                    if let Err(error) = visit {
                        tracing::error!(%error, "unable to save gym visit");
                        return reply(
                            StatusCode::INTERNAL_SERVER_ERROR,
                            json!({"error": "Unable to save gym visit."}),
                        );
                    }
                }
                saved_workouts.push(row);
            }
            Err(error) => {
                tracing::error!(%error, "unable to save workouts");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save workouts."}),
                );
            }
        }
    }
    match save_rings(&mut tx, user.0, &payload).await {
        Ok(rings) => {
            if let Err(error) = tx.commit().await {
                tracing::error!(%error, "unable to commit fitness activity rings");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save activity rings."}),
                );
            }
            reply(
                StatusCode::CREATED,
                json!({"rings": rings, "workouts": saved_workouts, "saved": true}),
            )
        }
        Err(error) => {
            tracing::error!(%error, "unable to save fitness activity rings");
            reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to save activity rings."}),
            )
        }
    }
}

async fn save_rings(
    tx: &mut sqlx::Transaction<'_, Postgres>,
    user_id: i64,
    payload: &Value,
) -> Result<SavedRings, sqlx::Error> {
    let (move_value, move_goal) = metric(payload.get("move"), "kcal", false).unwrap();
    let (exercise, exercise_goal) = metric(payload.get("exercise"), "min", true).unwrap();
    let (stand, stand_goal) = metric(payload.get("stand"), "hr", true).unwrap();
    sqlx::query_as::<_, SavedRings>(
        "INSERT INTO fitness_activity_rings (user_id, summary_date, move_calories, move_calories_goal, \
         exercise_minutes, exercise_minutes_goal, stand_hours, stand_hours_goal, steps_count, updated_at) \
         VALUES ($1, $2, $3::double precision::numeric, $4::double precision::numeric, \
         $5::bigint, $6::bigint, $7::bigint, $8::bigint, $9::bigint, NOW()) \
         ON CONFLICT (user_id, summary_date) DO UPDATE SET move_calories = EXCLUDED.move_calories, \
         move_calories_goal = EXCLUDED.move_calories_goal, exercise_minutes = EXCLUDED.exercise_minutes, \
         exercise_minutes_goal = EXCLUDED.exercise_minutes_goal, stand_hours = EXCLUDED.stand_hours, \
         stand_hours_goal = EXCLUDED.stand_hours_goal, steps_count = EXCLUDED.steps_count, \
         updated_at = EXCLUDED.updated_at \
         RETURNING id, summary_date, move_calories::double precision AS move_calories, \
         move_calories_goal::double precision AS move_calories_goal, exercise_minutes, \
         exercise_minutes_goal, stand_hours, stand_hours_goal, steps_count, created_at, updated_at"
    )
    .bind(user_id)
    .bind(NaiveDate::parse_from_str(payload["summary_date"].as_str().unwrap(), "%Y-%m-%d").unwrap())
    .bind(move_value).bind(move_goal)
    .bind(exercise as i64).bind(exercise_goal as i64)
    .bind(stand as i64).bind(stand_goal as i64)
    .bind(integer(payload.get("step_count")).unwrap())
    .fetch_one(&mut **tx).await
}

#[cfg(test)]
mod tests {
    use super::*;

    fn example() -> Value {
        json!({
            "summary_date": "2026-07-26", "time_zone": "Asia/Kolkata",
            "captured_at": "2026-07-26T04:04:00Z",
            "move": {"value": 180.45, "goal": 350, "unit": "kcal"},
            "exercise": {"value": 35, "goal": 40, "unit": "min"},
            "stand": {"value": 9, "goal": 9, "unit": "hr"}, "step_count": 7500,
            "workouts": [{"uuid": "6B29FC40-CA47-1000-8000-00805F9B34FB",
                "activity_type_raw": 50, "activity_type_name": "Traditional Strength Training",
                "start_date": "2026-07-26T16:38:43Z", "end_date": "2026-07-26T17:14:11Z",
                "duration_seconds": 2127.667, "calories_burned_kcal": 180.455,
                "distance_meters": null, "metadata": null}]
        })
    }

    #[test]
    fn nested_payload_validation_keeps_nextjs_errors() {
        let payload = example();
        assert_eq!(validate(&payload), Ok(()));
        let mut invalid = payload.clone();
        invalid["step_count"] = json!(7500.5);
        assert_eq!(
            validate(&invalid),
            Err("step_count is required and must be a non-negative integer.".into())
        );
        invalid["step_count"] = serde_json::from_str("7500.0").unwrap();
        assert_eq!(validate(&invalid), Ok(()));
        invalid = payload.clone();
        invalid["workouts"][0]["metadata"] = json!([]);
        assert_eq!(
            validate(&invalid),
            Err("workouts[0] metadata must be an object, null, or omitted.".into())
        );
    }

    #[tokio::test]
    async fn saves_and_upserts_with_nextjs_response_shape() {
        let database = crate::testhelper::TestDatabase::start().await;
        let pool = database.pool.clone();
        sqlx::raw_sql(
            "CREATE TEMP TABLE workout_activity_types (id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, raw_value int UNIQUE NOT NULL, name varchar(100) NOT NULL); \
             CREATE TEMP TABLE fitness_workouts (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, uuid uuid NOT NULL, UNIQUE (user_id, uuid), activity_type_id int NOT NULL, \
             start_time timestamptz NOT NULL, end_time timestamptz NOT NULL, duration_seconds numeric(10,3) NOT NULL, \
             calories_burned numeric(8,2), distance_meters numeric(10,2), metadata jsonb, updated_at timestamptz DEFAULT NOW()); \
             CREATE TEMP TABLE gym_visits (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, fitness_workout_id bigint UNIQUE, \
             created_at timestamptz NOT NULL DEFAULT NOW(), updated_at timestamptz NOT NULL DEFAULT NOW()); \
             CREATE TEMP TABLE exercise_catalog (id text PRIMARY KEY); \
             CREATE TEMP TABLE gym_visit_exercises (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, gym_visit_id bigint REFERENCES gym_visits(id), \
             name text NOT NULL, exercise_catalog_id text REFERENCES exercise_catalog(id)); \
             CREATE TEMP TABLE gym_exercise_sets (gym_visit_exercise_id bigint REFERENCES gym_visit_exercises(id), set_number int, reps int, weight numeric); \
             CREATE TEMP TABLE fitness_activity_rings (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, summary_date date NOT NULL, UNIQUE (user_id, summary_date), \
             move_calories numeric(8,2) NOT NULL, move_calories_goal numeric(8,2) NOT NULL, exercise_minutes int NOT NULL, \
             exercise_minutes_goal int NOT NULL, stand_hours int NOT NULL, stand_hours_goal int NOT NULL, steps_count int NOT NULL, \
             created_at timestamptz DEFAULT NOW(), updated_at timestamptz DEFAULT NOW())"
        ).execute(&pool).await.unwrap();
        let state = AppState { pool: pool.clone() };
        let mut payload = example();
        let mut second_workout = payload["workouts"][0].clone();
        second_workout["uuid"] = json!("7B29FC40-CA47-1000-8000-00805F9B34FB");
        second_workout["start_date"] = json!("2026-07-26T17:00:00Z");
        second_workout["end_date"] = json!("2026-07-26T17:30:00Z");
        payload["workouts"]
            .as_array_mut()
            .unwrap()
            .push(second_workout);
        let body = Bytes::from(serde_json::to_vec(&payload).unwrap());
        let (status, Json(first)) =
            create(State(state.clone()), Extension(ApiKeyUser(1)), body.clone()).await;
        assert_eq!(status, StatusCode::CREATED, "{first}");
        assert_eq!(first["saved"], true);
        assert_eq!(first["rings"]["summary_date"], "2026-07-26");
        assert_eq!(first["rings"]["move_calories"], 180.45);
        assert_eq!(
            first["workouts"][0]["uuid"],
            "6b29fc40-ca47-1000-8000-00805f9b34fb"
        );
        // A repeated Apple workout import must preserve exercises saved by Go/Hermes.
        sqlx::raw_sql(
            "INSERT INTO exercise_catalog VALUES ('Dumbbell_Shoulder_Press'); \
             INSERT INTO gym_visit_exercises (gym_visit_id, name, exercise_catalog_id) \
             SELECT id, 'dumbbell shoulder press', 'Dumbbell_Shoulder_Press' FROM gym_visits WHERE user_id = 1; \
             INSERT INTO gym_exercise_sets SELECT id, 1, 12, 7.5 FROM gym_visit_exercises",
        )
        .execute(&pool)
        .await
        .unwrap();
        let (status, Json(second)) =
            create(State(state.clone()), Extension(ApiKeyUser(1)), body.clone()).await;
        assert_eq!(status, StatusCode::CREATED, "{second}");
        assert_eq!(first["rings"]["id"], second["rings"]["id"]);
        assert_eq!(first["workouts"][0]["id"], second["workouts"][0]["id"]);
        let saved: (String, String, i32, f64) = sqlx::query_as(
            "SELECT e.name, e.exercise_catalog_id, s.reps, s.weight::double precision \
             FROM gym_visit_exercises e JOIN gym_exercise_sets s ON s.gym_visit_exercise_id = e.id \
             JOIN gym_visits v ON v.id = e.gym_visit_id WHERE v.user_id = 1",
        )
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(
            saved,
            (
                "dumbbell shoulder press".into(),
                "Dumbbell_Shoulder_Press".into(),
                12,
                7.5
            )
        );
        let (visits, linked): (i64, i64) = sqlx::query_as(
            "SELECT COUNT(*), COUNT(fitness_workout_id) FROM gym_visits WHERE user_id = 1",
        )
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!((visits, linked), (1, 1));
        let (workout_id, created_at): (i64, DateTime<Utc>) = sqlx::query_as(
            "SELECT fitness_workout_id, created_at FROM gym_visits WHERE user_id = 1",
        )
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(workout_id, first["workouts"][0]["id"].as_i64().unwrap());
        assert_eq!(
            created_at,
            date_time(payload["workouts"][0].get("start_date")).unwrap()
        );
        let (status, Json(other)) =
            create(State(state.clone()), Extension(ApiKeyUser(2)), body).await;
        assert_eq!(status, StatusCode::CREATED, "{other}");
        assert_ne!(first["rings"]["id"], other["rings"]["id"]);
        assert_ne!(first["workouts"][0]["id"], other["workouts"][0]["id"]);
        let visits: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM gym_visits WHERE user_id = 2")
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(visits, 1);
        sqlx::query("INSERT INTO gym_visits (user_id, created_at) VALUES (3, $1)")
            .bind(date_time(payload["workouts"][0].get("start_date")).unwrap())
            .execute(&pool)
            .await
            .unwrap();
        let body = Bytes::from(serde_json::to_vec(&payload).unwrap());
        let (status, _) = create(State(state), Extension(ApiKeyUser(3)), body).await;
        assert_eq!(status, StatusCode::CREATED);
        let (visits, linked): (i64, i64) = sqlx::query_as(
            "SELECT COUNT(*), COUNT(fitness_workout_id) FROM gym_visits WHERE user_id = 3",
        )
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!((visits, linked), (1, 0));

        let mut invalid = example();
        invalid["move"]["value"] = json!(100_000_000);
        invalid["workouts"][0]["uuid"] = json!("1b29fc40-ca47-1000-8000-00805f9b34fb");
        invalid["workouts"][0]["activity_type_raw"] = json!(21);
        let state = AppState { pool: pool.clone() };
        let (status, _) = create(
            State(state),
            Extension(ApiKeyUser(1)),
            Bytes::from(serde_json::to_vec(&invalid).unwrap()),
        )
        .await;
        assert_eq!(status, StatusCode::INTERNAL_SERVER_ERROR);
        let activity_type_exists: bool = sqlx::query_scalar(
            "SELECT EXISTS (SELECT 1 FROM workout_activity_types WHERE raw_value = 21)",
        )
        .fetch_one(&pool)
        .await
        .unwrap();
        let workout_exists: bool =
            sqlx::query_scalar("SELECT EXISTS (SELECT 1 FROM fitness_workouts WHERE uuid = $1)")
                .bind(Uuid::parse_str("1b29fc40-ca47-1000-8000-00805f9b34fb").unwrap())
                .fetch_one(&pool)
                .await
                .unwrap();
        assert!(!activity_type_exists && !workout_exists);
    }
}
