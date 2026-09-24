# VERBOSE=1 to see commands
ifeq ($(VERBOSE),1)
	SILENT =
else
	SILENT = @
endif

.PHONY: test test-integration cover sqlc-generate

sqlc-generate:
	go run github.com/sqlc-dev/sqlc/cmd/sqlc@v1.30.0 generate

## Tests
test:
	@echo "🧪 Running unit tests"
	$(SILENT) go test ./...

test-integration:
	@echo "🧪 Running tests including integration (Docker)"
	$(SILENT) go test -v -tags=integration ./...

cover:
	@echo "📈 Coverage"
	$(SILENT) go test -coverprofile=coverage.out ./... && go tool cover -func=coverage.out

