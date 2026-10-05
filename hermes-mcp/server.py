"""MCP tools for the PULSE API."""

import json
import os
from pathlib import Path
from datetime import date, datetime, timedelta, timezone
from typing import TypedDict
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from urllib.parse import urlencode

from mcp.server import MCPServer


_release_path = Path("/release.json")
if not _release_path.exists():
    _release_path = Path(__file__).resolve().parent.parent / "release.json"
_release = json.loads(_release_path.read_text())
mcp = MCPServer("pulse", version=_release["version"])
_session_token = None
_india_time = timezone(timedelta(hours=5, minutes=30))


class ExerciseSet(TypedDict):
    reps: int
    weight: float | None


class ExerciseDetails(TypedDict):
    name: str
    exercise_catalog_id: str | None
    sets: list[ExerciseSet]


class ExerciseInput(ExerciseDetails, total=False):
    remember_alias: bool


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
        raise ValueError("PULSE login did not return a session cookie")
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
        raise ValueError("Invalid workout response from PULSE")
    return json.dumps(result)


@mcp.tool()
def get_vehicles() -> str:
    """List the vehicles in my garage. Read-only; returns each vehicle's name and ID."""
    result = _get("/api/vehicles")
    if not isinstance(result, list) or not all(
        isinstance(item, dict) and isinstance(item.get("name"), str) for item in result
    ):
        raise ValueError("Invalid vehicle response from PULSE")
    return json.dumps(result)


@mcp.tool()
def search_exercises(names: list[str]) -> str:
    """Look up exercise names before saving a workout. Returns exact/remembered matches or up to eight candidates with IDs, equipment and instructions. Use only returned IDs. Suggested matches need the user's choice; ask about equipment or seated/standing variants when ambiguous. After clarification, search again with the equipment and alternate exercise names if needed (e.g. machine rear delt fly is also called reverse machine flyes). Never infer equipment from weights. A name with no suitable match can be saved with exercise_catalog_id=null."""
    if not isinstance(names, list) or not 1 <= len(names) <= 20:
        raise ValueError("Provide between 1 and 20 exercise names")
    if any(not isinstance(name, str) or not 1 <= len(name.strip()) <= 100 for name in names):
        raise ValueError("Each exercise name must be between 1 and 100 characters")
    results = []
    for name in names:
        result = _get("/api/exercise-catalog?" + urlencode({"q": name.strip()}))
        if not isinstance(result, dict) or not isinstance(result.get("candidates"), list):
            raise ValueError("Invalid exercise catalogue response from PULSE")
        results.append(result)
    return json.dumps(results)


@mcp.tool()
def add_exercises_to_workout(exercises: list[ExerciseInput], confirmed: bool = False,
                             workout_date: str | None = None) -> str:
    """Save exercises to a gym visit after search_exercises and explicit user confirmation. Preserve their original name; pass the selected exercise_catalog_id from search, or null if unmatched. Set remember_alias=true only if the user asks to remember this wording for future workouts. Show the target date, selected catalogue variants, and every kg/reps set before confirmation. Ask about ambiguous variants; never infer equipment from weights or multiply dumbbell weights. If no date is given, use today's visit in India time; otherwise pass YYYY-MM-DD in workout_date. Never infer missing numbers or retry an uncertain write. Weight is kilograms; use null only for explicitly unweighted sets."""
    if confirmed is not True:
        raise ValueError("Show the extracted exercises and sets to the user and get confirmation before saving")
    if not isinstance(exercises, list) or not 1 <= len(exercises) <= 20:
        raise ValueError("Provide between 1 and 20 exercises")
    for exercise in exercises:
        if not isinstance(exercise, dict) or not isinstance(exercise.get("name"), str) or not exercise["name"].strip():
            raise ValueError("Each exercise needs a name")
        if "exercise_catalog_id" not in exercise:
            raise ValueError("Search exercises first and provide exercise_catalog_id or explicit null")
        catalog_id = exercise["exercise_catalog_id"]
        if catalog_id is not None and (not isinstance(catalog_id, str) or not 1 <= len(catalog_id.strip()) <= 200):
            raise ValueError("exercise_catalog_id must be a catalogue ID or null")
        remember = exercise.get("remember_alias", False)
        if type(remember) is not bool or (remember and catalog_id is None):
            raise ValueError("Remembering an alias requires a selected catalogue exercise")
        sets = exercise.get("sets")
        if not isinstance(sets, list) or not 1 <= len(sets) <= 20:
            raise ValueError("Each exercise needs between 1 and 20 sets")
        for item in sets:
            if not isinstance(item, dict) or type(item.get("reps")) is not int or not 1 <= item["reps"] <= 1000:
                raise ValueError("Each set needs a positive rep count")
            weight = item.get("weight")
            if "weight" not in item or (weight is not None and (type(weight) not in (int, float) or not 0 <= weight <= 999999.99)):
                raise ValueError("Each set needs a non-negative weight in kg or explicit null")
    if workout_date is None:
        visit = _get("/api/workout/today")
        if not isinstance(visit, dict) or visit.get("visited") is not True or type(visit.get("id")) is not int:
            raise ValueError("No gym visit is recorded for today; record the visit before saving exercises")
        visit_id = visit["id"]
    else:
        try:
            target = date.fromisoformat(workout_date)
            if target.isoformat() != workout_date:
                raise ValueError
        except (TypeError, ValueError):
            raise ValueError("workout_date must be a valid date in YYYY-MM-DD format") from None
        visits = _get("/api/gym-visits")
        if not isinstance(visits, list):
            raise ValueError("Invalid gym visit list from PULSE")
        matches = []
        for visit in visits:
            if not isinstance(visit, dict) or type(visit.get("id")) is not int or not isinstance(visit.get("created_at"), str):
                raise ValueError("Invalid gym visit list from PULSE")
            try:
                created_at = datetime.fromisoformat(visit["created_at"].replace("Z", "+00:00"))
                if created_at.tzinfo is None:
                    raise ValueError
            except ValueError:
                raise ValueError("Invalid gym visit date from PULSE") from None
            if created_at.astimezone(_india_time).date() == target:
                matches.append(visit["id"])
        if not matches:
            raise ValueError(f"No gym visit is recorded for {workout_date}")
        if len(matches) > 1:
            raise ValueError(f"Multiple gym visits are recorded for {workout_date}; choose a specific visit")
        visit_id = matches[0]
    return json.dumps(_request(f"/api/gym-visits/{visit_id}/exercises/batch",
                               method="POST", body={"exercises": exercises}))


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host="0.0.0.0", port=8000,
            stateless_http=True, json_response=True)
