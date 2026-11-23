use std::process::Stdio;
use std::time::{Duration, Instant};

use tokio::process::Command;

use sqlx::{Connection, PgConnection, PgPool, postgres::PgPoolOptions};

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
        assert!(
            output.status.success(),
            "discover test PostgreSQL port for {}: {}",
            container.0,
            String::from_utf8_lossy(&output.stderr)
        );
        let address = String::from_utf8(output.stdout).unwrap();
        let address = address.trim();
        let url = format!("postgres://pulse:pass@{address}/pulse_test?sslmode=disable");
        let deadline = tokio::time::Instant::now() + Duration::from_secs(60);
        loop {
            // A direct connection preserves socket errors that a pool retries internally.
            let attempt_deadline =
                deadline.min(tokio::time::Instant::now() + Duration::from_secs(2));
            let error = match tokio::time::timeout_at(attempt_deadline, PgConnection::connect(&url))
                .await
            {
                Ok(Ok(_)) => break,
                Ok(Err(error)) => error.to_string(),
                Err(_) => "connection attempt timed out".to_owned(),
            };
            if tokio::time::Instant::now() >= deadline {
                let mut logs = Command::new("docker");
                logs.args(["logs", "--tail", "20", &container.0])
                    .kill_on_drop(true);
                let logs = match tokio::time::timeout(Duration::from_secs(5), logs.output()).await {
                    Ok(Ok(output)) => format!(
                        "{}{}",
                        String::from_utf8_lossy(&output.stdout),
                        String::from_utf8_lossy(&output.stderr)
                    ),
                    Ok(Err(error)) => error.to_string(),
                    Err(_) => "docker logs timed out".to_owned(),
                };
                panic!(
                    "test PostgreSQL at {address} did not become ready within 60 seconds: {error}\n{logs}"
                );
            }
            tokio::time::sleep_until(
                deadline.min(tokio::time::Instant::now() + Duration::from_millis(200)),
            )
            .await;
        }
        Self {
            pool: PgPoolOptions::new()
                .max_connections(1)
                .acquire_timeout(Duration::from_secs(2))
                .connect_lazy(&url)
                .expect("valid test PostgreSQL connection URL"),
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
