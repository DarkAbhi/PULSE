//! Startup settings read from the process environment, with credentials redacted from diagnostics.

use std::{env, fmt, io};

pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub rust_log: String,
}

impl fmt::Debug for Config {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Config")
            .field("database_url", &"[redacted]")
            .field("port", &self.port)
            .field("rust_log", &self.rust_log)
            .finish()
    }
}

impl Config {
    pub fn from_env() -> Result<Self, io::Error> {
        let database_url = required("DATABASE_URL")?;
        let port = env::var("PORT").unwrap_or_else(|_| "8083".to_owned());
        let port = port.parse().map_err(|error| {
            io::Error::new(
                io::ErrorKind::InvalidInput,
                format!("invalid PORT {port:?}: {error}"),
            )
        })?;
        let rust_log = env::var("RUST_LOG").unwrap_or_else(|_| "info".to_owned());

        Ok(Self {
            database_url,
            port,
            rust_log,
        })
    }
}

fn required(name: &str) -> Result<String, io::Error> {
    env::var(name)
        .ok()
        .filter(|value| !value.is_empty())
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, format!("{name} must be set")))
}

#[cfg(test)]
mod tests {
    use super::Config;

    #[test]
    fn debug_output_redacts_database_credentials() {
        let config = Config {
            database_url: "postgres://secret-user:secret-password@localhost/db".into(),
            port: 8083,
            rust_log: "info".into(),
        };
        let debug = format!("{config:?}");
        assert!(
            !debug.contains("secret-user") && !debug.contains("secret-password"),
            "{debug}"
        );
        assert!(debug.contains("[redacted]"), "{debug}");
    }
}
