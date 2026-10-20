use std::{env, io};

pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub rust_log: String,
}

impl Config {
    pub fn from_env() -> Result<Self, io::Error> {
        let database_url = required("DATABASE_URL")?;
        let port = env::var("PORT")
            .unwrap_or_else(|_| "8083".to_owned())
            .parse()
            .map_err(|_| io::Error::new(io::ErrorKind::InvalidInput, "PORT must be a valid u16"))?;
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
