use std::process::Stdio;
use std::time::{Duration, Instant};

use tokio::process::Command;

use sqlx::{PgPool, postgres::PgPoolOptions};

#[derive(Debug)]
pub struct TestDatabase {
    pub pool: PgPool,
    _container: TestContainer,
}

impl TestDatabase {
    pub async fn start() -> Self {
        let output = docker(&[
            "run",
            "--detach",
            "--rm",
            "--label",
            "life-backend-rust-tests",
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
        .await;
        assert!(
            output.status.success(),
            "start test PostgreSQL: {}",
            String::from_utf8_lossy(&output.stderr)
        );
        // Install cleanup before port discovery or connection can fail.
        let container = TestContainer(String::from_utf8(output.stdout).unwrap().trim().to_owned());
        let output = docker(&["port", &container.0, "5432/tcp"]).await;
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

#[derive(Debug)]
struct TestContainer(String);

async fn docker(args: &[&str]) -> std::process::Output {
    let mut command = Command::new("docker");
    command.args(args).kill_on_drop(true);
    tokio::time::timeout(Duration::from_secs(30), command.output())
        .await
        .expect("Docker command exceeded 30 seconds")
        .expect("database tests require a working Docker executable")
}

impl Drop for TestContainer {
    fn drop(&mut self) {
        let id = self.0.clone();
        // Tokio waits for blocking cleanup tasks when each test runtime shuts down.
        tokio::task::spawn_blocking(move || {
            if let Err(error) = remove_container(&id) {
                tracing::warn!(%error, container = id, "test PostgreSQL cleanup failed");
            }
        });
    }
}

fn remove_container(id: &str) -> std::io::Result<()> {
    let mut child = std::process::Command::new("docker")
        .args(["rm", "--force", id])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()?;
    let started = Instant::now();
    loop {
        if let Some(status) = child.try_wait()? {
            return if status.success() {
                Ok(())
            } else {
                Err(std::io::Error::other(format!("docker rm failed: {status}")))
            };
        }
        if started.elapsed() >= Duration::from_secs(30) {
            child.kill()?;
            child.wait()?;
            return Err(std::io::Error::new(
                std::io::ErrorKind::TimedOut,
                "docker rm exceeded 30 seconds",
            ));
        }
        std::thread::sleep(Duration::from_millis(20));
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn cleanup_removes_container_after_a_test_task_panics() {
        let database = TestDatabase::start().await;
        let id = database._container.0.clone();
        let failure = tokio::spawn(async move {
            let _database = database;
            panic!("intentional test failure to exercise container cleanup");
        })
        .await
        .expect_err("test task must panic");
        assert!(failure.is_panic());
        tokio::time::timeout(Duration::from_secs(10), async {
            while docker(&["inspect", &id]).await.status.success() {
                tokio::time::sleep(Duration::from_millis(50)).await;
            }
        })
        .await
        .expect("failed test must remove its database container");
    }
}
