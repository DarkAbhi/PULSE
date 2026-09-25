.PHONY: build up down logs restart deploy ps docker-migrate-up docker-rollback docker-steps docker-version backend-logs web-logs bot-logs dev dev-down dev-logs go-test go-test-integration go-cover monitoring-logs

# ----- PROD -----
build:
	COMPOSE_PROFILES=prod docker compose build

up:
	COMPOSE_PROFILES=prod docker compose up -d --build --remove-orphans

down:
	COMPOSE_PROFILES=prod docker compose down --remove-orphans

logs:
	COMPOSE_PROFILES=prod docker compose logs -f --tail=200

backend-logs:
	COMPOSE_PROFILES=prod docker compose logs -f --tail=200 backend

web-logs:
	COMPOSE_PROFILES=prod docker compose logs -f --tail=200 web

bot-logs:
	COMPOSE_PROFILES=prod docker compose logs -f --tail=200 bot

ps:
	COMPOSE_PROFILES=prod docker compose ps

restart:
	COMPOSE_PROFILES=prod docker compose down --remove-orphans && \
	COMPOSE_PROFILES=prod docker compose up -d --build

deploy: build up
	@echo "✅ Deployed"

# ----- Migrations (prod image) -----
docker-migrate-up:
	@echo "🔼 Running all pending migrations in docker"
	@COMPOSE_PROFILES=prod docker compose run --rm --build migrate --migrate-up

docker-rollback:
	@echo "↩️  Rolling back last migration in docker"
	@COMPOSE_PROFILES=prod docker compose run --rm --build migrate --rollback

docker-steps:
	@echo "🔂 Running docker migration steps: $(n)"
	@test -n "$(n)" || (echo "provide n, e.g. make docker-steps n=-2"; exit 1)
	@COMPOSE_PROFILES=prod docker compose run --rm --build migrate --steps=$(n)

docker-version:
	@echo "📌 Showing docker migration version"
	@COMPOSE_PROFILES=prod docker compose run --rm --build migrate --version

# ----- DEV -----
dev:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d --remove-orphans backend-dev web-dev bot-dev prometheus grafana jaeger

dev-down:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml --profile dev down --remove-orphans

dev-logs:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f --tail=200 backend-dev web-dev bot-dev

# ----- DEV migrations -----
dev-migrate-up:
	@echo "🔼 Running all pending migrations (dev)"
	@docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --build migrate --migrate-up

dev-rollback:
	@echo "↩️  Rolling back last migration (dev)"
	@docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --build migrate --rollback

# Usage: make dev-steps n=-2  (negative = rollback; positive = apply)
dev-steps:
	@echo "🔂 Running dev migration steps: $(n)"
	@test -n "$(n)" || (echo "provide n, e.g. make dev-steps n=-2"; exit 1)
	@docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --build migrate --steps=$(n)

dev-version:
	@echo "📌 Showing dev migration version"
	@docker compose -f docker-compose.yml -f docker-compose.dev.yml run --rm --build migrate --version

# ----- Observability & Monitoring -----
monitoring-logs:
	@docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f --tail=100 prometheus grafana jaeger

