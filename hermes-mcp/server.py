"""Read-only MCP tools for the Life Tracker API."""

import json
import os
from urllib.request import urlopen

from mcp.server import MCPServer


mcp = MCPServer("life-tracker")


def _get(path):
    base = os.environ.get("LIFE_BACKEND_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
    with urlopen(f"{base}{path}", timeout=10) as response:
        return json.load(response)


@mcp.tool()
def get_workout_today() -> str:
    """Check if I worked out today (India time). Read-only; returns visited and optional visit ID."""
    result = _get("/api/workout/today")
    if not isinstance(result, dict) or not isinstance(result.get("visited"), bool):
        raise ValueError("Invalid workout response from Life Tracker")
    return json.dumps(result)


@mcp.tool()
def get_vehicles() -> str:
    """List the vehicles in my garage. Read-only; returns each vehicle's name and ID."""
    result = _get("/api/vehicles")
    if not isinstance(result, list) or not all(
        isinstance(item, dict) and isinstance(item.get("name"), str) for item in result
    ):
        raise ValueError("Invalid vehicle response from Life Tracker")
    return json.dumps(result)


if __name__ == "__main__":
    mcp.run(transport="stdio")
