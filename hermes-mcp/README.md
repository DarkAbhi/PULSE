# Life Tracker MCP server

This standalone stdio MCP server exposes only two read-only tools: `get_workout_today` and `get_vehicles`. Any MCP client can use it. Hermes uses its own Telegram bot token; the existing button bot remains separate.

## Ubuntu setup

At the current Hermes installer screen, choose **Walk through all configurations**. Set up Telegram there, then leave unrelated integrations off. If it offers MCP server setup now, skip adding a server until this checkout is on Ubuntu; the config below adds it afterwards. Use a new BotFather token for Hermes and allowlist only your Telegram user ID. Hermes's Docker terminal backend does not run this MCP process; Hermes starts it on the host.

Deploy Life Tracker first. The production Compose file publishes the Go API only on `127.0.0.1:8080`. Verify it from Ubuntu with `curl -fsS http://127.0.0.1:8080/healthz`.

From the Life Tracker repository on Ubuntu, install the MCP Python SDK in its own environment:

```sh
python3 -m venv hermes-mcp/.venv
hermes-mcp/.venv/bin/python -m pip install -r hermes-mcp/requirements.txt
```

Add this server under `mcp_servers` in `~/.hermes/config.yaml`, replacing `/absolute/path/to/life-backend` with the real checkout path:

```yaml
mcp_servers:
  life_tracker:
    command: /absolute/path/to/life-backend/hermes-mcp/.venv/bin/python
    args: [/absolute/path/to/life-backend/hermes-mcp/server.py]
    tools:
      include: [get_workout_today, get_vehicles]
      prompts: false
      resources: false
```

Test with `hermes mcp test life_tracker`, then start or restart the gateway. Ask the Hermes bot “Did I work out today?” and “What vehicles are in my garage?” If the backend uses a different host-side address, set `LIFE_BACKEND_BASE_URL` under this server's `env` mapping. Hermes does not pass arbitrary host environment variables to MCP subprocesses.

For an always-on Ubuntu bot, use `hermes gateway install`, `hermes gateway start`, and `hermes gateway status`; `sudo loginctl enable-linger "$USER"` keeps the user service running after logout. Production and development Compose both use host port 8080, so run one stack at a time.
