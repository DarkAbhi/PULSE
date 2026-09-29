use axum::{Json, body::Bytes, extract::State, http::StatusCode};
use chrono::NaiveDate;
use serde_json::{Map, Value, json};

use crate::{
    db::activity_rings::{self, Rings},
    state::AppState,
};

type Reply = (StatusCode, Json<Value>);

pub async fn create(State(state): State<AppState>, body: Bytes) -> Reply {
    let Ok(payload) = serde_json::from_slice::<Value>(&body) else {
        return reply(
            StatusCode::BAD_REQUEST,
            json!({"error": "Request body must be valid JSON."}),
        );
    };
    let rings = match parse(payload) {
        Ok(rings) => rings,
        Err(details) => {
            return reply(
                StatusCode::BAD_REQUEST,
                json!({
                    "error": "The fitness rings payload is invalid.", "details": details
                }),
            );
        }
    };

    if let Some(zone) = &rings.time_zone {
        match activity_rings::time_zone_exists(&state.pool, zone).await {
            Ok(false) => {
                return reply(
                    StatusCode::BAD_REQUEST,
                    json!({
                        "error": "The fitness rings payload is invalid.",
                        "details": "time_zone must be a valid IANA time zone."
                    }),
                );
            }
            Err(error) => {
                tracing::error!(%error, "unable to validate time zone");
                return reply(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    json!({"error": "Unable to save activity rings."}),
                );
            }
            Ok(true) => {}
        }
    }

    match activity_rings::upsert(&state.pool, &rings).await {
        Ok(()) => reply(StatusCode::CREATED, json!({"saved": true})),
        Err(error) => {
            tracing::error!(%error, "unable to save fitness activity rings");
            reply(
                StatusCode::INTERNAL_SERVER_ERROR,
                json!({"error": "Unable to save activity rings."}),
            )
        }
    }
}

fn reply(status: StatusCode, body: Value) -> Reply {
    (status, Json(body))
}

fn parse(payload: Value) -> Result<Rings, &'static str> {
    let Value::Object(object) = payload else {
        return Err("Request body must be a JSON object.");
    };
    let normalized: Map<String, Value> = object
        .into_iter()
        .map(|(key, value)| (key.trim().to_owned(), value))
        .collect();

    let summary_date = normalized
        .get("summary_date")
        .and_then(Value::as_str)
        .filter(|value| {
            value.len() == 10 && value.as_bytes()[4] == b'-' && value.as_bytes()[7] == b'-'
        })
        .and_then(|value| NaiveDate::parse_from_str(value, "%Y-%m-%d").ok())
        .ok_or("summary_date must be a valid YYYY-MM-DD date.")?;
    let time_zone = normalized
        .get("time_zone")
        .map(|value| {
            value
                .as_str()
                .map(str::to_owned)
                .ok_or("time_zone must be a valid IANA time zone.")
        })
        .transpose()?;

    let number = |key| normalized.get(key).and_then(non_negative_number);
    let integer = |key| normalized.get(key).and_then(non_negative_integer);
    Ok(Rings {
        summary_date,
        move_calories: number("move")
            .ok_or("move is required and must be a non-negative number.")?,
        move_calories_goal: number("move_goal")
            .ok_or("move_goal is required and must be a non-negative number.")?,
        exercise_minutes: integer_alias(&normalized, "exercise", "exercise_minutes")
            .ok_or("exercise is required and must be a non-negative integer.")?,
        exercise_minutes_goal: integer("exercise_goal")
            .ok_or("exercise_goal is required and must be a non-negative integer.")?,
        stand_hours: integer_alias(&normalized, "stand_hours", "stand")
            .ok_or("stand_hours is required and must be a non-negative integer.")?,
        stand_hours_goal: integer("stand_goal")
            .ok_or("stand_goal is required and must be a non-negative integer.")?,
        steps_count: integer_alias(&normalized, "steps_count", "step_count")
            .ok_or("steps_count is required and must be a non-negative integer.")?,
        time_zone,
    })
}

fn integer_alias(values: &Map<String, Value>, first: &str, second: &str) -> Option<i64> {
    values
        .get(first)
        .filter(|value| !value.is_null())
        .or_else(|| values.get(second))
        .and_then(non_negative_integer)
}

fn non_negative_number(value: &Value) -> Option<f64> {
    let number = match value {
        Value::Number(value) => value.as_f64()?,
        Value::String(value) if !value.trim().is_empty() => value.trim().parse().ok()?,
        _ => return None,
    };
    (number.is_finite() && number >= 0.0).then_some(number)
}

fn non_negative_integer(value: &Value) -> Option<i64> {
    let number = non_negative_number(value)?;
    (number.fract() == 0.0 && number <= i64::MAX as f64).then(|| number as i64)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn flat_payload_matches_shortcut_contract() {
        let payload = json!({
            "time_zone": "Asia/Kolkata", "stand_goal": 9, "exercise ": 0,
            "summary_date": "2026-07-27", "steps_count": 106, "move_goal": "350",
            "exercise_goal": 40, "move": 38.763, "stand_hours": 2
        });
        let rings = parse(payload).unwrap();
        assert_eq!(rings.move_calories_goal, 350.0);
        assert_eq!(rings.exercise_minutes, 0);
        assert_eq!(rings.steps_count, 106);

        let alias = json!({
            "summary_date": "2026-07-27", "move": "150.5", "move_goal": 300,
            "exercise_minutes": 30, "exercise_goal": "30", "stand": 10,
            "stand_goal": 12, "step_count": 5000
        });
        assert_eq!(parse(alias).unwrap().steps_count, 5000);
        assert_eq!(
            parse(json!({"summary_date": "invalid"})).err(),
            Some("summary_date must be a valid YYYY-MM-DD date.")
        );
        assert_eq!(
            parse(json!({
                "summary_date": "2026-07-27", "time_zone": 5
            }))
            .err(),
            Some("time_zone must be a valid IANA time zone.")
        );
        assert_eq!(non_negative_integer(&json!(-1)), None);
        assert_eq!(non_negative_integer(&json!(10.5)), None);
    }

    #[tokio::test]
    async fn malformed_body_has_original_error_response() {
        let config = crate::config::Config {
            database_url: "postgres://127.0.0.1:1/test".to_owned(),
            api_token: String::new(),
            port: 0,
            rust_log: String::new(),
        };
        let pool = sqlx::postgres::PgPoolOptions::new()
            .connect_lazy(&config.database_url)
            .unwrap();
        let (status, Json(body)) = create(
            State(AppState { pool, config }),
            Bytes::from_static(b"not json"),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
        assert_eq!(body, json!({"error": "Request body must be valid JSON."}));
    }
}
