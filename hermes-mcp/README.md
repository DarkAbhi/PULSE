# Life Tracker MCP server

The production Compose stack runs this MCP server beside the Go backend. It exposes `get_workout_today`, `get_vehicles`, `search_exercises`, and `add_exercises_to_workout`.

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
      include: [get_workout_today, get_vehicles, search_exercises, add_exercises_to_workout]
      prompts: false
      resources: false
```

Run `hermes mcp test life_tracker`, then `hermes gateway restart`. In the existing Telegram chat, send `/reload-mcp` to refresh that session's tools, then ask “Did I work out today?” and “What vehicles are in my garage?” The CLI test checks protocol discovery; the Telegram questions also check that the tools can reach the backend. If discovery fails, inspect `docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod logs mcp`.

For workout messages, Hermes must extract the exercises into `name` and ordered `sets` with numeric `reps` and `weight` in kilograms. If the user names a workout date, Hermes must pass `workout_date` as `YYYY-MM-DD` (for example, "29th September" means `2026-09-29` when the intended year is 2026). Without a stated date, leave `workout_date` unset so the tool uses today's visit in India time. Hermes must show the target date and complete extracted workout, then ask for explicit confirmation before calling `add_exercises_to_workout` with `confirmed=true`. It must ask about missing or ambiguous dates or numbers instead of guessing. The tool requires an existing visit on the target date and rejects dates with multiple visits. After an uncertain network failure, check saved exercises before trying again; a repeated write can create duplicates.

Before that summary, call `search_exercises` with the extracted names. Use the returned `exercise_catalog_id` for unique exact or remembered matches. For suggestions, show the relevant equipment and variant and obtain the user's choice; ask whether a "rear delt fly" used a machine, cables, or dumbbells instead of inferring it from the weights. Include the selected catalogue name in the confirmation summary. Each saved exercise must include `exercise_catalog_id` (a returned ID or explicit `null` if no suitable match). Preserve the original `name` and all weights exactly as reported, including the user's per-dumbbell convention. Do not silently double weights.

Search ranks names, so synonyms may need another lookup. After equipment or
variant clarification, search again using that detail and alternate names: for
example, search "reverse machine flyes" after the user confirms a machine rear
delt fly. Preserve "rear delt fly" as the saved original name. Never choose an
unrelated candidate just because it is the first search result.

Set `remember_alias=true` only if the user explicitly asks to remember that wording for future workouts. The backend saves aliases for the authenticated account, in the same transaction as the exercises and sets. Names with no chosen ID still resolve unique exact/remembered matches; fuzzy suggestions are never linked automatically.

Example after the user confirms the plate-raise match and asks to remember it:

```json
{
  "exercises": [{
    "name": "weight plate front raises",
    "exercise_catalog_id": "Front_Plate_Raise",
    "remember_alias": true,
    "sets": [{"reps": 10, "weight": 10}, {"reps": 10, "weight": 10}, {"reps": 10, "weight": 10}]
  }],
  "confirmed": true
}
```

Existing installations must add `search_exercises` to their tool allowlist above, restart the gateway, and send `/reload-mcp` in Telegram after deploying. Apply database migration 23 before using the new tools. See [catalogue setup](../go-backend/data/exercise-catalog.md).
