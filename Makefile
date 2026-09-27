.PHONY: build up down logs restart deploy ps docker-migrate-up docker-rollback docker-steps docker-version backend-logs web-logs bot-logs dev dev-down dev-logs go-test go-test-integration go-cover monitoring-logs dev-monitoring-logs

PROD_COMPOSE = docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod
DEV_COMPOSE = docker compose --env-file .env.dev --project-name life-dev -f docker-compose.yml -f docker-compose.dev.yml --profile dev

# ----- PROD -----
build:
	$(PROD_COMPOSE) build

up:
	$(PROD_COMPOSE) up -d --build --remove-orphans

down:
	$(PROD_COMPOSE) down --remove-orphans

logs:
	$(PROD_COMPOSE) logs -f --tail=200

backend-logs:
	$(PROD_COMPOSE) logs -f --tail=200 backend

web-logs:
	$(PROD_COMPOSE) logs -f --tail=200 web

bot-logs:
	$(PROD_COMPOSE) logs -f --tail=200 bot

ps:
	$(PROD_COMPOSE) ps

restart:
	$(PROD_COMPOSE) down --remove-orphans && \
	$(PROD_COMPOSE) up -d --build

deploy: build up
	@echo "✅ Deployed"

# ----- Migrations (prod image) -----
docker-migrate-up:
	@echo "🔼 Running all pending migrations in docker"
	@$(PROD_COMPOSE) run --rm --build migrate --migrate-up

docker-rollback:
	@echo "↩️  Rolling back last migration in docker"
	@$(PROD_COMPOSE) run --rm --build migrate --rollback

docker-steps:
	@echo "🔂 Running docker migration steps: $(n)"
	@test -n "$(n)" || (echo "provide n, e.g. make docker-steps n=-2"; exit 1)
	@$(PROD_COMPOSE) run --rm --build migrate --steps=$(n)

docker-version:
	@echo "📌 Showing docker migration version"
	@$(PROD_COMPOSE) run --rm --build migrate --version

# ----- DEV -----
dev:
	$(DEV_COMPOSE) up -d --remove-orphans backend-dev web-dev bot-dev prometheus grafana jaeger

dev-down:
	$(DEV_COMPOSE) down --remove-orphans

dev-logs:
	$(DEV_COMPOSE) logs -f --tail=200 backend-dev web-dev bot-dev

# ----- DEV migrations -----
dev-migrate-up:
	@echo "🔼 Running all pending migrations (dev)"
	@$(DEV_COMPOSE) run --rm --build migrate-dev --migrate-up

dev-rollback:
	@echo "↩️  Rolling back last migration (dev)"
	@$(DEV_COMPOSE) run --rm --build migrate-dev --rollback

# Usage: make dev-steps n=-2  (negative = rollback; positive = apply)
dev-steps:
	@echo "🔂 Running dev migration steps: $(n)"
	@test -n "$(n)" || (echo "provide n, e.g. make dev-steps n=-2"; exit 1)
	@$(DEV_COMPOSE) run --rm --build migrate-dev --steps=$(n)

dev-version:
	@echo "📌 Showing dev migration version"
	@$(DEV_COMPOSE) run --rm --build migrate-dev --version

# ----- Observability & Monitoring -----
monitoring-logs:
	@$(PROD_COMPOSE) logs -f --tail=100 prometheus grafana jaeger

dev-monitoring-logs:
	@$(DEV_COMPOSE) logs -f --tail=100 prometheus grafana jaeger
