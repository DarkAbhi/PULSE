# Life Tracker MCP server

The production Compose stack runs this read-only MCP server beside the Go backend. It exposes only `get_workout_today` and `get_vehicles`. Hermes has its own Telegram bot; the existing button bot remains separate.

## Ubuntu setup

Deploy the updated repository with `make deploy`. Compose connects the MCP service to `http://backend:8080` internally and publishes its HTTP endpoint only on the host's loopback interface at `http://127.0.0.1:18082/mcp`. Hermes itself stays on the host; its Docker terminal backend is unrelated to this connection.

Verify the backend from Ubuntu:

```sh
curl -fsS http://127.0.0.1:18080/api/workout/today
curl -fsS http://127.0.0.1:18080/api/vehicles
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

Run `hermes mcp test life_tracker`, then `hermes gateway restart`. Ask the Hermes Telegram bot “Did I work out today?” and “What vehicles are in my garage?” The test checks protocol discovery; the Telegram questions also check that the tools can reach the backend. If discovery fails, inspect `docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod logs mcp`.
