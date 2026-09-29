FROM rust:1.98.1-bookworm AS build
ENV RUSTUP_TOOLCHAIN=1.98.1
WORKDIR /app
COPY Cargo.toml Cargo.lock rust-toolchain.toml ./
COPY src ./src
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/app/target \
    cargo build --release --locked && \
    cp target/release/pulse-rust-backend /app/pulse-rust-backend

FROM debian:bookworm-slim
COPY --from=build /app/pulse-rust-backend /usr/local/bin/pulse-rust-backend
USER 65532:65532
EXPOSE 8083
ENTRYPOINT ["pulse-rust-backend"]
