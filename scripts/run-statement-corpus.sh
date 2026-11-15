#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
corpus="$(cd "${1:-"$repo/test-statement"}" && pwd)"
scratch="$(mktemp -d)"
project="pulse-statement-test-$(basename "$scratch" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"
chmod 700 "$scratch"
# Compose gives exported shell variables precedence over values in --env-file.
unset APP_ENV DB_HOSTNAME DB_NAME DB_PORT DB_SSLMODE DB_USERNAME DB_PASSWORD
unset STATEMENT_TEST_PASSWORD STATEMENT_EXTRACTOR_URL GRAFANA_ADMIN_PASSWORD
unset AWS_REGION AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN S3_ENDPOINT S3_BUCKET S3_FORCE_PATH_STYLE
export CORPUS_DIR="$corpus"
export PULSE_DEV_SEED_FILE=./go-backend/data/statement-test-seed.sql
# Let Docker assign loopback ports so the normal development stack can stay up.
export PULSE_DEV_GO_BIND=127.0.0.1:0 PULSE_DEV_RUST_BIND=127.0.0.1:0 PULSE_DEV_WEB_BIND=127.0.0.1:0
export PULSE_DEV_PROMETHEUS_BIND=127.0.0.1:0 PULSE_DEV_GRAFANA_BIND=127.0.0.1:0 PULSE_DEV_JAEGER_BIND=127.0.0.1:0
compose=(docker compose --project-name "$project" --env-file "$scratch/env"
  -f "$repo/docker-compose.yml" -f "$repo/docker-compose.dev.yml"
  --profile dev --profile statement-test)
started=0
cleanup() {
  if [[ "$started" = 1 ]]; then
    "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -f "$scratch/env"
  rmdir "$scratch"
}
trap cleanup EXIT

python3 - "$scratch/env" <<'PY'
from pathlib import Path
import secrets
import sys
path = Path(sys.argv[1])
path.touch(mode=0o600)
path.write_text(
    "APP_ENV=development\nDB_HOSTNAME=statement-test-db\nDB_NAME=statement_test\n"
    "DB_PORT=5432\nDB_SSLMODE=disable\nDB_USERNAME=statement_test\n"
    "DB_PASSWORD=" + secrets.token_hex(24) + "\n"
    "STATEMENT_TEST_PASSWORD=" + secrets.token_hex(24) + "\n"
    "GRAFANA_ADMIN_PASSWORD=" + secrets.token_hex(24) + "\n"
)
PY

"${compose[@]}" config --quiet
docker info --format '{{.ServerVersion}}' >/dev/null
started=1
# The normal dev migrator also works with an external database, so wait here for the test one.
"${compose[@]}" up -d --wait --wait-timeout 90 statement-test-db
"${compose[@]}" up -d --build --wait --wait-timeout 300 \
  statement-extractor backend-dev rust-backend-dev web-dev prometheus grafana jaeger loki alloy
"${compose[@]}" run --rm --no-deps statement-corpus
