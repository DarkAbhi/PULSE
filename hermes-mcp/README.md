# Life Tracker MCP server

The production Compose stack runs this read-only MCP server beside the Go backend. It exposes only `get_workout_today` and `get_vehicles`.

## Ubuntu setup

Set `MCP_BACKEND_USERNAME` and `MCP_BACKEND_PASSWORD` in the server's ignored `.env` file to the Life Tracker account whose gym and garage data Hermes should read. Keep these credentials out of Hermes's config and chat messages. The MCP server logs in to `/api/auth/login`, keeps the session token in memory, and logs in again after a 401 (for example when the 30-day session expires).

Deploy the updated repository with `make deploy`. Compose connects the MCP service to `http://backend:8080` internally and publishes its HTTP endpoint only on the host's loopback interface at `http://127.0.0.1:18082/mcp`. Hermes itself stays on the host; its Docker terminal backend is unrelated to this connection.

Verify that unauthenticated requests are rejected from Ubuntu (both should print `401`):

```sh
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/workout/today
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/vehicles
```

Add this entry to `~/.hermes/config.yaml`. If `mcp_servers:` already exists, add only the `life_tracker:` entry beneath it:

```yaml
mcp_servers:
  life_tracker:
    url: http://127.0.0.1:18082/mcp
    tools:
      include: [get_workout_today, get_vehicles]
      prompts: false
      resources: false
```

Run `hermes mcp test life_tracker`, then `hermes gateway restart`. In the existing Telegram chat, send `/reload-mcp` to refresh that session's tools, then ask “Did I work out today?” and “What vehicles are in my garage?” The CLI test checks protocol discovery; the Telegram questions also check that the tools can reach the backend. If discovery fails, inspect `docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod logs mcp`.
