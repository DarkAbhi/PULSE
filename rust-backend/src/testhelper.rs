use std::process::Command;
use std::time::Duration;

use sqlx::{PgPool, postgres::PgPoolOptions};

pub struct TestDatabase {
    pub pool: PgPool,
    _container: TestContainer,
}

impl TestDatabase {
    pub async fn start() -> Self {
        let output = Command::new("docker")
            .args([
                "run",
                "--detach",
                "--rm",
                "--publish",
                "127.0.0.1::5432",
                "--env",
                "POSTGRES_PASSWORD=pass",
                "--env",
                "POSTGRES_USER=pulse",
                "--env",
                "POSTGRES_DB=pulse_test",
                "postgres:17.6",
            ])
            .output()
            .expect("database tests require a running Docker daemon");
        assert!(
            output.status.success(),
            "start test PostgreSQL: {}",
            String::from_utf8_lossy(&output.stderr)
        );
        // Install cleanup before port discovery or connection can fail.
        let container = TestContainer(String::from_utf8(output.stdout).unwrap().trim().to_owned());
        let output = Command::new("docker")
            .args(["port", &container.0, "5432/tcp"])
            .output()
            .expect("discover test PostgreSQL port");
        assert!(output.status.success(), "discover test PostgreSQL port");
        let address = String::from_utf8(output.stdout).unwrap();
        let url = format!(
            "postgres://pulse:pass@{}/pulse_test?sslmode=disable",
            address.trim()
        );
        let pool = tokio::time::timeout(Duration::from_secs(60), async {
            loop {
                if let Ok(pool) = PgPoolOptions::new()
                    .max_connections(1)
                    .acquire_timeout(Duration::from_secs(2))
                    .connect(&url)
                    .await
                {
                    break pool;
                }
                tokio::time::sleep(Duration::from_millis(200)).await;
            }
        })
        .await
        .expect("test PostgreSQL did not become ready within 60 seconds");
        Self {
            pool,
            _container: container,
        }
    }
}

struct TestContainer(String);

impl Drop for TestContainer {
    fn drop(&mut self) {
        let _ = Command::new("docker")
            .args(["rm", "--force", &self.0])
            .output();
    }
}
