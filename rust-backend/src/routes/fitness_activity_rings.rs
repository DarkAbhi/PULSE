use axum::{Extension, Json, body::Bytes, extract::State, http::StatusCode};
use chrono::{DateTime, NaiveDate, Utc};
use serde::Serialize;
use serde_json::{Value, json};
use sqlx::Postgres;
use std::collections::{BTreeMap, HashMap, HashSet};
use uuid::Uuid;

use super::fitness_input::{self, MAX_CALORIES, MAX_DISTANCE, MAX_DURATION};
use crate::{
    auth::ApiKeyUser,
    db::activity_rings::{self, Rings},
    state::AppState,
};

struct Import {
    rings: Rings,
    workouts: Vec<Workout>,
}

struct Workout {
    uuid: Uuid,
    raw: i32,
    name: String,
    start: DateTime<Utc>,
    end: DateTime<Utc>,
    duration: f64,
    calories: Option<f64>,
    distance: Option<f64>,
    metadata: Value,
}

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

fn invalid(details: &str) -> Reply {
    reply(
        StatusCode::BAD_REQUEST,
        json!({
            "error": "The activity-rings payload is invalid.", "details": details
        }),
    )
}

fn date_time(value: Option<&Value>) -> Option<DateTime<Utc>> {
    DateTime::parse_from_rfc3339(value?.as_str()?)
        .ok()
        .map(|date| date.with_timezone(&Utc))
}

fn metric<'a>(payload: &'a Value, key: &str, unit: &str) -> Option<&'a Value> {
    let value = payload.get(key)?;
    (value.get("unit")?.as_str()? == unit).then_some(value)
}

fn decimal_metric(payload: &Value) -> Option<(f64, f64)> {
    let value = metric(payload, "move", "kcal")?;
    Some((
        fitness_input::number(value.get("value")?.as_f64()?, MAX_CALORIES)?,
        fitness_input::number(value.get("goal")?.as_f64()?, MAX_CALORIES)?,
    ))
}

fn integer_metric(payload: &Value, key: &str, unit: &str) -> Option<(i32, i32)> {
    let value = metric(payload, key, unit)?;
    Some((
        fitness_input::integer(value.get("value")?.as_f64()?)?,
        fitness_input::integer(value.get("goal")?.as_f64()?)?,
    ))
}

fn optional_number(
    value: Option<&Value>,
    max: f64,
    message: &'static str,
) -> Result<Option<f64>, &'static str> {
    value
        .filter(|value| !value.is_null())
        .map(|value| {
            value
                .as_f64()
                .and_then(|value| fitness_input::number(value, max))
                .ok_or(message)
        })
        .transpose()
}

fn parse_workout(value: &Value) -> Result<Workout, &'static str> {
    if !value.is_object() {
        return Err("must be an object.");
    }
    let uuid = value
        .get("uuid")
        .and_then(Value::as_str)
        .filter(|uuid| uuid.len() == 36)
        .and_then(|uuid| Uuid::parse_str(uuid).ok())
        .ok_or("uuid must be a UUID string.")?;
    let raw = value
        .get("activity_type_raw")
        .and_then(Value::as_f64)
        .and_then(fitness_input::integer)
        .ok_or("activity_type_raw must be a non-negative integer.")?;
    let name = value
        .get("activity_type_name")
        .and_then(Value::as_str)
        .filter(|name| !name.trim().is_empty() && name.encode_utf16().count() <= 100)
        .ok_or("activity_type_name must be a non-empty string of up to 100 characters.")?;
    let dates = date_time(value.get("start_date")).zip(date_time(value.get("end_date")));
    let (start, end) = dates.filter(|(start, end)| end >= start).ok_or(
        "start_date and end_date must be valid ISO date-times, with end_date after start_date.",
    )?;
    let duration = value
        .get("duration_seconds")
        .and_then(Value::as_f64)
        .and_then(|value| fitness_input::number(value, MAX_DURATION))
        .ok_or("duration_seconds must be a non-negative number.")?;
    let calories = optional_number(
        value.get("calories_burned_kcal"),
        MAX_CALORIES,
        "calories_burned_kcal must be a non-negative number or null.",
    )?;
    let distance = optional_number(
        value.get("distance_meters"),
        MAX_DISTANCE,
        "distance_meters must be a non-negative number or null.",
    )?;
    let metadata = value.get("metadata").filter(|value| !value.is_null());
    if metadata.is_some_and(|value| !value.is_object()) {
        return Err("metadata must be an object, null, or omitted.");
    }
    Ok(Workout {
        uuid,
        raw,
        name: name.trim().to_owned(),
        start,
        end,
        duration,
        calories,
        distance,
        metadata: metadata.cloned().unwrap_or_else(|| json!({})),
    })
}

fn parse(payload: &Value) -> Result<Import, String> {
    if !payload.is_object() {
        return Err("Request body must be a JSON object.".into());
    }
    if date_time(payload.get("captured_at")).is_none() {
        return Err("captured_at must be a valid ISO date-time.".into());
    }
    let summary_date = payload
        .get("summary_date")
        .and_then(Value::as_str)
        .filter(|date| date.len() == 10 && date.as_bytes()[4] == b'-' && date.as_bytes()[7] == b'-')
        .and_then(|date| NaiveDate::parse_from_str(date, "%Y-%m-%d").ok())
        .ok_or("summary_date must be a valid YYYY-MM-DD date.")?;
    let time_zone = payload
        .get("time_zone")
        .map(|zone| {
            zone.as_str()
                .map(str::to_owned)
                .ok_or("time_zone must be a valid IANA time zone.")
        })
        .transpose()?;
    let (move_calories, move_calories_goal) = decimal_metric(payload)
        .ok_or("move must include non-negative value and goal numbers with unit 'kcal'.")?;
    let (exercise_minutes, exercise_minutes_goal) = integer_metric(payload, "exercise", "min")
        .ok_or(
            "exercise must include non-negative integer value and goal values with unit 'min'.",
        )?;
    let (stand_hours, stand_hours_goal) = integer_metric(payload, "stand", "hr")
        .ok_or("stand must include non-negative integer value and goal values with unit 'hr'.")?;
    let steps_count = payload
        .get("step_count")
        .and_then(Value::as_f64)
        .and_then(fitness_input::integer)
        .ok_or("step_count is required and must be a non-negative integer.")?;
    let mut workouts = Vec::new();
    let mut uuids = HashSet::new();
    if let Some(values) = payload.get("workouts") {
        let values = values.as_array().ok_or("workouts must be an array.")?;
        for (index, value) in values.iter().enumerate() {
            let workout =
                parse_workout(value).map_err(|message| format!("workouts[{index}] {message}"))?;
            if !uuids.insert(workout.uuid) {
                return Err(format!(
                    "workouts[{index}] uuid must be unique within the request."
                ));
            }
            workouts.push(workout);
        }
    }
    Ok(Import {
        rings: Rings {
            summary_date,
            move_calories,
            move_calories_goal,
            exercise_minutes,
            exercise_minutes_goal,
            stand_hours,
            stand_hours_goal,
            steps_count,
            time_zone,
        },
        workouts,
    })
}

#[cfg(test)]
fn validate(payload: &Value) -> Result<(), String> {
    parse(payload).map(|_| ())
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
    let import = match parse(&payload) {
        Ok(import) => import,
        Err(details) => return invalid(&details),
    };
    if let Some(zone) = &import.rings.time_zone {
        match activity_rings::time_zone_exists(&state.pool, zone).await {
            Ok(true) => {}
            Ok(false) => return invalid("time_zone must be a valid IANA time zone."),
            Err(error) => {
                tracing::error!(%error, "unable to validate time zone");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save activity rings."}),
                );
            }
        }
    }
    // All imports acquire shared activity-type rows in the same order.
    let mut activity_types = BTreeMap::new();
    for workout in &import.workouts {
        activity_types.insert(workout.raw, workout.name.as_str());
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
             ON CONFLICT (raw_value) DO UPDATE SET name = workout_activity_types.name RETURNING id",
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
    if import.workouts.iter().any(|workout| workout.raw == 50) {
        // shortcut: one lock per user; use per-day locks if uploads become contentious.
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
    for workout in &import.workouts {
        let raw = workout.raw;
        let Some(&activity_type_id) = activity_type_ids.get(&raw) else {
            return reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to match workout activity types."}),
            );
        };
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
        .bind(workout.uuid)
        .bind(activity_type_id)
        .bind(workout.start)
        .bind(workout.end)
        .bind(workout.duration)
        .bind(workout.calories)
        .bind(workout.distance)
        .bind(sqlx::types::Json(&workout.metadata))
        .fetch_one(&mut *tx).await;
        match saved {
            Ok(row) => {
                if raw == 50 {
                    let visit = sqlx::query(
                        "INSERT INTO gym_visits (user_id, fitness_workout_id, created_at, updated_at) \
                         SELECT $1, $2, $3, $3 WHERE NOT EXISTS ( \
                             SELECT 1 FROM gym_visits WHERE user_id = $1 \
                             AND (created_at AT TIME ZONE 'Asia/Kolkata')::date = \
                                 ($3::timestamptz AT TIME ZONE 'Asia/Kolkata')::date) \
                         ON CONFLICT (fitness_workout_id) DO NOTHING",
                    )
                    .bind(user.0)
                    .bind(row.id)
                    .bind(workout.start)
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
    match save_rings(&mut tx, user.0, &import.rings).await {
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
    rings: &Rings,
) -> Result<SavedRings, sqlx::Error> {
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
    .bind(rings.summary_date)
    .bind(rings.move_calories).bind(rings.move_calories_goal)
    .bind(rings.exercise_minutes).bind(rings.exercise_minutes_goal)
    .bind(rings.stand_hours).bind(rings.stand_hours_goal)
    .bind(rings.steps_count)
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
    fn rejects_database_overflows_and_duplicate_workouts() {
        let mut payload = example();
        payload["step_count"] = json!(2_147_483_648_i64);
        assert!(validate(&payload).is_err(), "accepted {payload}");
        for (field, value) in [
            ("activity_type_raw", json!(2_147_483_648_i64)),
            ("duration_seconds", json!(10_000_000)),
            ("calories_burned_kcal", json!(1_000_000)),
            ("distance_meters", json!(100_000_000)),
        ] {
            let mut payload = example();
            payload["workouts"][0][field] = value;
            assert!(validate(&payload).is_err(), "accepted {payload}");
        }
        let mut payload = example();
        let mut duplicate = payload["workouts"][0].clone();
        duplicate["uuid"] = json!("6b29fc40-ca47-1000-8000-00805f9b34fb");
        payload["workouts"].as_array_mut().unwrap().push(duplicate);
        assert!(validate(&payload).is_err(), "accepted duplicate UUIDs");
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

    async fn setup(pool: &sqlx::PgPool) {
        sqlx::raw_sql(
            "CREATE TABLE workout_activity_types (id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, raw_value int UNIQUE NOT NULL, name varchar(100) NOT NULL); \
             CREATE TABLE fitness_workouts (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, uuid uuid NOT NULL, UNIQUE (user_id, uuid), activity_type_id int NOT NULL, \
             start_time timestamptz NOT NULL, end_time timestamptz NOT NULL, duration_seconds numeric(10,3) NOT NULL, \
             calories_burned numeric(8,2), distance_meters numeric(10,2), metadata jsonb, updated_at timestamptz DEFAULT NOW()); \
             CREATE TABLE gym_visits (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, fitness_workout_id bigint UNIQUE, \
             created_at timestamptz NOT NULL DEFAULT NOW(), updated_at timestamptz NOT NULL DEFAULT NOW()); \
             CREATE TABLE exercise_catalog (id text PRIMARY KEY); \
             CREATE TABLE gym_visit_exercises (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, gym_visit_id bigint REFERENCES gym_visits(id), \
             name text NOT NULL, exercise_catalog_id text REFERENCES exercise_catalog(id)); \
             CREATE TABLE gym_exercise_sets (gym_visit_exercise_id bigint REFERENCES gym_visit_exercises(id), set_number int, reps int, weight numeric); \
             CREATE TABLE fitness_activity_rings (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, user_id bigint NOT NULL, summary_date date NOT NULL, UNIQUE (user_id, summary_date), \
             move_calories numeric(8,2) NOT NULL, move_calories_goal numeric(8,2) NOT NULL, exercise_minutes int NOT NULL, \
             exercise_minutes_goal int NOT NULL, stand_hours int NOT NULL, stand_hours_goal int NOT NULL, steps_count int NOT NULL, \
             created_at timestamptz DEFAULT NOW(), updated_at timestamptz DEFAULT NOW())"
        ).execute(pool).await.unwrap();
    }

    #[tokio::test]
    async fn saves_and_upserts_with_nextjs_response_shape() {
        let database = crate::testhelper::TestDatabase::start().await;
        let pool = database.pool.clone();
        setup(&pool).await;
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
        sqlx::query("ALTER TABLE fitness_activity_rings ADD CONSTRAINT test_storage_failure CHECK (move_calories < 200)")
            .execute(&pool).await.unwrap();
        invalid["move"]["value"] = json!(250);
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
        sqlx::query("ALTER TABLE fitness_activity_rings DROP CONSTRAINT test_storage_failure")
            .execute(&pool)
            .await
            .unwrap();

        let mut renamed = example();
        renamed["workouts"][0]["activity_type_name"] = json!("Changed by user 2");
        let (status, Json(response)) = create(
            State(AppState { pool: pool.clone() }),
            Extension(ApiKeyUser(2)),
            Bytes::from(serde_json::to_vec(&renamed).unwrap()),
        )
        .await;
        assert_eq!(status, StatusCode::CREATED, "{response}");
        let name: String = sqlx::query_scalar("SELECT a.name FROM fitness_workouts w JOIN workout_activity_types a ON a.id=w.activity_type_id WHERE w.user_id=1 LIMIT 1")
            .fetch_one(&pool).await.unwrap();
        assert_eq!(name, "Traditional Strength Training");

        let mut corrected = example();
        corrected["workouts"][0]["start_date"] = json!("2026-07-27T16:38:43Z");
        corrected["workouts"][0]["end_date"] = json!("2026-07-27T17:14:11Z");
        let (status, Json(response)) = create(
            State(AppState { pool: pool.clone() }),
            Extension(ApiKeyUser(1)),
            Bytes::from(serde_json::to_vec(&corrected).unwrap()),
        )
        .await;
        assert_eq!(status, StatusCode::CREATED, "{response}");
        let visit: (DateTime<Utc>, i64) = sqlx::query_as("SELECT v.created_at, COUNT(s.*) FROM gym_visits v JOIN gym_visit_exercises e ON e.gym_visit_id=v.id JOIN gym_exercise_sets s ON s.gym_visit_exercise_id=e.id WHERE v.user_id=1 GROUP BY v.id")
            .fetch_one(&pool).await.unwrap();
        assert_eq!(
            visit,
            (created_at, 1),
            "saved gym data must survive date correction"
        );
        let start: DateTime<Utc> =
            sqlx::query_scalar("SELECT start_time FROM fitness_workouts WHERE id=$1")
                .bind(workout_id)
                .fetch_one(&pool)
                .await
                .unwrap();
        assert_eq!(
            Some(start),
            date_time(corrected["workouts"][0].get("start_date"))
        );
    }
    #[tokio::test]
    async fn concurrent_imports_preserve_names_and_finish_without_deadlocks() {
        let database = crate::testhelper::TestDatabase::start().await;
        let pool = sqlx::postgres::PgPoolOptions::new()
            .max_connections(4)
            .connect_with((*database.pool.connect_options()).clone())
            .await
            .unwrap();
        setup(&pool).await;
        let mut payload = example();
        let mut other = payload["workouts"][0].clone();
        other["uuid"] = json!("7B29FC40-CA47-1000-8000-00805F9B34FB");
        other["activity_type_raw"] = json!(21);
        other["activity_type_name"] = json!("Cycling");
        payload["workouts"].as_array_mut().unwrap().push(other);
        let mut reversed = payload.clone();
        reversed["workouts"].as_array_mut().unwrap().reverse();
        for round in 0..4 {
            let (left, right) = tokio::time::timeout(std::time::Duration::from_secs(10), async {
                tokio::join!(
                    create(
                        State(AppState { pool: pool.clone() }),
                        Extension(ApiKeyUser(1)),
                        Bytes::from(serde_json::to_vec(&payload).unwrap())
                    ),
                    create(
                        State(AppState { pool: pool.clone() }),
                        Extension(ApiKeyUser(2)),
                        Bytes::from(serde_json::to_vec(&reversed).unwrap())
                    )
                )
            })
            .await
            .expect("concurrent imports must finish within 10 seconds");
            assert_eq!(left.0, StatusCode::CREATED, "round {round}: {:?}", left.1);
            assert_eq!(right.0, StatusCode::CREATED, "round {round}: {:?}", right.1);
            assert_eq!(
                left.1.0["workouts"][0]["uuid"],
                "6b29fc40-ca47-1000-8000-00805f9b34fb"
            );
            assert_eq!(
                right.1.0["workouts"][0]["uuid"],
                "7b29fc40-ca47-1000-8000-00805f9b34fb"
            );
        }
        let count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM fitness_workouts")
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(count, 4);
        pool.close().await;
    }

    #[tokio::test]
    async fn invalid_payloads_return_400_before_connecting_to_database() {
        let pool = sqlx::postgres::PgPoolOptions::new()
            .connect_lazy("postgres://127.0.0.1:1/test")
            .unwrap();
        let mut overflow = example();
        overflow["step_count"] = json!(2_147_483_648_i64);
        let mut duplicate = example();
        let workout = duplicate["workouts"][0].clone();
        duplicate["workouts"].as_array_mut().unwrap().push(workout);
        for payload in [overflow, duplicate] {
            let (status, Json(response)) = create(
                State(AppState { pool: pool.clone() }),
                Extension(ApiKeyUser(1)),
                Bytes::from(serde_json::to_vec(&payload).unwrap()),
            )
            .await;
            assert_eq!(status, StatusCode::BAD_REQUEST, "{response}");
        }
    }
}
