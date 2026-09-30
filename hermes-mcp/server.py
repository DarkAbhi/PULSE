"""Read-only MCP tools for the Life Tracker API."""

import json
import os
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from mcp.server import MCPServer


mcp = MCPServer("life-tracker")
_session_token = None


def _login(base):
    username = os.environ.get("MCP_BACKEND_USERNAME")
    password = os.environ.get("MCP_BACKEND_PASSWORD")
    if not username or not password:
        raise RuntimeError("Set MCP_BACKEND_USERNAME and MCP_BACKEND_PASSWORD for the MCP service")
    body = json.dumps({"username": username, "password": password}).encode()
    request = Request(f"{base}/api/auth/login", data=body,
                      headers={"Content-Type": "application/json"})
    with urlopen(request, timeout=10) as response:
        cookies = SimpleCookie(response.headers.get("Set-Cookie", ""))
    if "life_session" not in cookies:
        raise ValueError("Life Tracker login did not return a session cookie")
    return cookies["life_session"].value


def _get(path):
    global _session_token
    base = os.environ.get("LIFE_BACKEND_BASE_URL", "http://127.0.0.1:18080").rstrip("/")
    for attempt in range(2):
        if not _session_token:
            _session_token = _login(base)
        request = Request(f"{base}{path}",
                          headers={"Authorization": f"Bearer {_session_token}"})
        try:
            with urlopen(request, timeout=10) as response:
                return json.load(response)
        except HTTPError as error:
            if error.code != 401 or attempt:
                raise
            _session_token = None


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
    mcp.run(transport="streamable-http", host="0.0.0.0", port=8000,
            stateless_http=True, json_response=True)
