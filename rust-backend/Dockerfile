FROM rust:1.98.1-bookworm AS build
ENV RUSTUP_TOOLCHAIN=1.98.1
WORKDIR /app
COPY Cargo.toml Cargo.lock rust-toolchain.toml ./
COPY src ./src
RUN cargo build --release --locked

FROM debian:bookworm-slim
COPY --from=build /app/target/release/pulse-rust-backend /usr/local/bin/pulse-rust-backend
USER 65532:65532
EXPOSE 8083
ENTRYPOINT ["pulse-rust-backend"]
