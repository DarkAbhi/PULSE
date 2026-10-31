FROM rust:1.98.1-bookworm AS build
ENV RUSTUP_TOOLCHAIN=1.98.1
WORKDIR /app
COPY rust-backend/Cargo.toml rust-backend/Cargo.lock rust-backend/rust-toolchain.toml ./
COPY release.json /release.json
COPY rust-backend/src ./src
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/app/target \
    cargo build --release --locked && \
    cp target/release/pulse-rust-backend /app/pulse-rust-backend

FROM debian:bookworm-slim
COPY --from=build /app/pulse-rust-backend /usr/local/bin/pulse-rust-backend
USER 65532:65532
EXPOSE 8083
ENTRYPOINT ["pulse-rust-backend"]
