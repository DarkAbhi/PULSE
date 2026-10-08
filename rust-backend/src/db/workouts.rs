use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::PgPool;

#[derive(Debug, Serialize)]
pub struct FitnessWorkout {
    pub start_time: DateTime<Utc>,
    pub duration_seconds: f64,
    pub distance_meters: Option<f64>,
    pub activity_type: ActivityType,
}

#[derive(Debug, Serialize)]
pub struct ActivityType {
    pub name: String,
}

#[derive(sqlx::FromRow)]
struct WorkoutRow {
    start_time: DateTime<Utc>,
    duration_seconds: f64,
    distance_meters: Option<f64>,
    activity_type_name: String,
}

pub async fn latest_relevant(pool: &PgPool) -> Result<Vec<FitnessWorkout>, sqlx::Error> {
    let rows: Vec<WorkoutRow> = sqlx::query_as(
        "WITH selected AS ( \
           (SELECT w.id FROM fitness_workouts w \
            JOIN workout_activity_types a ON a.id = w.activity_type_id \
            WHERE a.name ILIKE '%run%' AND w.distance_meters > 0 \
            ORDER BY w.start_time DESC, w.id DESC LIMIT 1) \
           UNION \
           (SELECT w.id FROM fitness_workouts w \
            JOIN workout_activity_types a ON a.id = w.activity_type_id \
            WHERE a.name ILIKE '%cycling%' AND w.distance_meters > 0 \
            ORDER BY w.start_time DESC, w.id DESC LIMIT 1) \
           UNION \
           (SELECT w.id FROM fitness_workouts w \
            JOIN workout_activity_types a ON a.id = w.activity_type_id \
            WHERE a.name ILIKE 'traditional strength training' \
               OR a.name ILIKE 'functional strength training' \
            ORDER BY w.start_time DESC, w.id DESC LIMIT 1) \
         ) \
         SELECT w.start_time, w.duration_seconds::double precision AS duration_seconds, \
                w.distance_meters::double precision AS distance_meters, \
                a.name AS activity_type_name \
         FROM selected s \
         JOIN fitness_workouts w ON w.id = s.id \
         JOIN workout_activity_types a ON a.id = w.activity_type_id \
         ORDER BY CASE WHEN a.name ILIKE 'traditional strength training' \
                         OR a.name ILIKE 'functional strength training' \
                       THEN 0 ELSE 1 END, w.start_time DESC, w.id DESC",
    )
    .fetch_all(pool)
    .await?;

    Ok(rows
        .into_iter()
        .map(|row| FitnessWorkout {
            start_time: row.start_time,
            duration_seconds: row.duration_seconds,
            distance_meters: row.distance_meters,
            activity_type: ActivityType {
                name: row.activity_type_name,
            },
        })
        .collect())
}

#[cfg(test)]
mod tests {
    use super::latest_relevant;
    use crate::db::activity_rings;

    #[tokio::test]
    async fn summary_selects_latest_strength_run_and_ride() {
        let database = crate::testhelper::TestDatabase::start().await;
        let pool = database.pool.clone();

        sqlx::query(
            "CREATE TEMP TABLE fitness_activity_rings (summary_date date, updated_at timestamptz, \
             move_calories numeric, move_calories_goal numeric, exercise_minutes int, \
             exercise_minutes_goal int, stand_hours int, stand_hours_goal int, steps_count int)",
        )
        .execute(&pool)
        .await
        .unwrap();
        sqlx::query("CREATE TEMP TABLE workout_activity_types (id int, name text)")
            .execute(&pool)
            .await
            .unwrap();
        sqlx::query(
            "CREATE TEMP TABLE fitness_workouts (id int, activity_type_id int, start_time timestamptz, \
             duration_seconds numeric, distance_meters numeric)",
        )
        .execute(&pool)
        .await
        .unwrap();
        sqlx::raw_sql(
            "INSERT INTO fitness_activity_rings VALUES \
             ('2026-07-25', '2026-07-25T18:00:00Z', 100.25, 350, 20, 40, 8, 12, 5000), \
             ('2026-07-26', '2026-07-26T18:00:00Z', 180.45, 350, 35, 40, 9, 12, 7500); \
             INSERT INTO workout_activity_types VALUES \
             (1, 'Functional Strength Training'), (2, 'Running'), (3, 'Cycling'), \
             (4, 'Traditional Strength Training'), (5, 'Walking'); \
             INSERT INTO fitness_workouts VALUES \
             (1, 2, '2026-01-01T00:00:00Z', 1800, 5000), \
             (2, 3, '2026-01-02T00:00:00Z', 3600, 20000), \
             (3, 2, '2026-01-03T00:00:00Z', 300, 0), \
             (4, 1, '2026-01-04T00:00:00Z', 1200, NULL), \
             (5, 4, '2026-01-05T00:00:00Z', 1800, NULL); \
             INSERT INTO fitness_workouts \
             SELECT n + 5, 5, '2026-02-01T00:00:00Z'::timestamptz + n * interval '1 day', 1200, NULL \
             FROM generate_series(1, 101) AS n",
        )
        .execute(&pool)
        .await
        .unwrap();

        let snapshot = activity_rings::latest(&pool).await.unwrap().unwrap();
        let workouts = latest_relevant(&pool).await.unwrap();
        let response = serde_json::json!({ "snapshot": snapshot, "workouts": workouts });

        assert_eq!(response["snapshot"]["summary_date"], "2026-07-26");
        assert_eq!(response["snapshot"]["move_calories"], 180.45);
        assert_eq!(response["workouts"].as_array().unwrap().len(), 3);
        assert_eq!(
            response["workouts"][0]["activity_type"]["name"],
            "Traditional Strength Training"
        );
        assert_eq!(response["workouts"][1]["activity_type"]["name"], "Cycling");
        assert_eq!(response["workouts"][2]["activity_type"]["name"], "Running");
        assert_eq!(response["workouts"][2]["distance_meters"], 5000.0);

        sqlx::query("DELETE FROM fitness_workouts WHERE id = 5")
            .execute(&pool)
            .await
            .unwrap();
        let workouts = latest_relevant(&pool).await.unwrap();
        assert_eq!(
            workouts[0].activity_type.name,
            "Functional Strength Training"
        );

        sqlx::query(
            "INSERT INTO fitness_workouts VALUES \
             (107, 3, '2026-07-26T00:00:00Z', 600, 1000)",
        )
        .execute(&pool)
        .await
        .unwrap();
        let workouts = latest_relevant(&pool).await.unwrap();
        assert_eq!(workouts.len(), 3);
        assert_eq!(
            workouts[0].activity_type.name,
            "Functional Strength Training"
        );
        assert_eq!(workouts[1].activity_type.name, "Cycling");
        assert_eq!(workouts[2].activity_type.name, "Running");

        sqlx::query("TRUNCATE fitness_activity_rings, fitness_workouts")
            .execute(&pool)
            .await
            .unwrap();
        assert!(activity_rings::latest(&pool).await.unwrap().is_none());
        assert!(latest_relevant(&pool).await.unwrap().is_empty());
    }
}
