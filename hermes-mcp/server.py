"""MCP tools for the PULSE API."""

import json
import os
from typing import TypedDict
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from mcp.server import MCPServer


mcp = MCPServer("life-tracker")
_session_token = None


class ExerciseSet(TypedDict):
    reps: int
    weight: float | None


class ExerciseInput(TypedDict):
    name: str
    sets: list[ExerciseSet]


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


def _request(path, method="GET", body=None):
    global _session_token
    base = os.environ.get("LIFE_BACKEND_BASE_URL", "http://127.0.0.1:18080").rstrip("/")
    for attempt in range(2):
        if not _session_token:
            _session_token = _login(base)
        headers = {"Authorization": f"Bearer {_session_token}"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        request = Request(f"{base}{path}",
                          data=json.dumps(body).encode() if body is not None else None,
                          headers=headers, method=method)
        try:
            with urlopen(request, timeout=10) as response:
                return json.load(response)
        except HTTPError as error:
            if error.code != 401 or attempt:
                raise
            _session_token = None


def _get(path):
    return _request(path)


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


@mcp.tool()
def add_exercises_to_today(exercises: list[ExerciseInput], confirmed: bool = False) -> str:
    """Save exercises to today's gym visit. First show the user every exercise and set with exact kg and reps; call only after they explicitly confirm that summary. Never infer missing numbers or retry an uncertain write. Weight is kilograms; use null only when the user explicitly says unweighted."""
    if confirmed is not True:
        raise ValueError("Show the extracted exercises and sets to the user and get confirmation before saving")
    if not isinstance(exercises, list) or not 1 <= len(exercises) <= 20:
        raise ValueError("Provide between 1 and 20 exercises")
    for exercise in exercises:
        if not isinstance(exercise, dict) or not isinstance(exercise.get("name"), str) or not exercise["name"].strip():
            raise ValueError("Each exercise needs a name")
        sets = exercise.get("sets")
        if not isinstance(sets, list) or not 1 <= len(sets) <= 20:
            raise ValueError("Each exercise needs between 1 and 20 sets")
        for item in sets:
            if not isinstance(item, dict) or type(item.get("reps")) is not int or not 1 <= item["reps"] <= 1000:
                raise ValueError("Each set needs a positive rep count")
            weight = item.get("weight")
            if "weight" not in item or (weight is not None and (type(weight) not in (int, float) or not 0 <= weight <= 999999.99)):
                raise ValueError("Each set needs a non-negative weight in kg or explicit null")
    visit = _get("/api/workout/today")
    if not isinstance(visit, dict) or visit.get("visited") is not True or type(visit.get("id")) is not int:
        raise ValueError("No gym visit is recorded for today; record the visit before saving exercises")
    return json.dumps(_request(f"/api/gym-visits/{visit['id']}/exercises/batch",
                               method="POST", body={"exercises": exercises}))


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host="0.0.0.0", port=8000,
            stateless_http=True, json_response=True)
