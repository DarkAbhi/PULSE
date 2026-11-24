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
_workout_types = {
    "single_muscle", "double_muscle", "push", "pull", "legs", "upper_body",
    "lower_body", "full_body", "cardio", "mobility", "other",
}


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
    """Check if I worked out today in my Pulse profile time zone. Read-only; returns visited and the latest visit ID if present."""
    result = _get("/api/workout/today")
    if not isinstance(result, dict) or not isinstance(result.get("visited"), bool):
        raise ValueError("Invalid workout response from PULSE")
    return json.dumps(result)


def _workout_visits(workout_date: str | None) -> tuple[str, list[dict]]:
    if workout_date is None:
        target = datetime.now(_india_time).date()
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
            matches.append(visit)
    return target.isoformat(), matches


@mcp.tool()
def get_workout_sessions(workout_date: str | None = None) -> str:
    """Read recorded workout sessions, type, focus, and exercises with sets for YYYY-MM-DD, or today in India time. Use before saving to confirm the date and session, and after an uncertain save to check what was recorded. If there are multiple visits, show their times and any distinguishing saved details and ask which visit to use."""
    target, visits = _workout_visits(workout_date)
    sessions = []
    for visit in visits:
        visit_id = visit["id"]
        details = _get(f"/api/fitness-workouts/{visit_id}")
        exercises = _get(f"/api/gym-visits/{visit_id}/exercises")
        if not isinstance(details, dict) or details.get("id") != visit_id or not isinstance(exercises, list):
            raise ValueError("Invalid workout session response from PULSE")
        sessions.append({**details, "exercises": exercises})
    return json.dumps({"workout_date": target, "sessions": sessions})


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
def add_exercises_to_workout(exercises: list[ExerciseInput] | None = None, confirmed: bool = False,
                             workout_date: str | None = None, workout_type: str | None = None,
                             workout_focus: str | None = None, visit_id: int | None = None) -> str:
    """Append exercises and/or save the type and focus of an existing gym visit, after explicit confirmation.

    Use get_workout_sessions to identify the visit. Confirm the YYYY-MM-DD date
    and session, then pass that date and visit_id. A missing date defaults to
    today in India time. If there are multiple visits, ask the user to choose.
    Show the proposed type and focus, selected catalogue variants, and every
    kg/reps set before confirmation. Clarify missing numbers, ambiguous
    exercise variants, or any requested classification that is unclear. Leave
    both classification fields unset for exercises alone. For classification
    alone, omit exercises.

    workout_type is single_muscle, double_muscle, push, pull, legs, upper_body,
    lower_body, full_body, cardio, mobility, or other. workout_focus is optional
    text up to 120 characters. Sending a type replaces both fields. Carry
    forward any saved field the user wants to keep, and confirm before clearing
    either. Empty focus clears the focus; empty type clears both. Rest days
    belong to the workout schedule.

    Look up exercises with search_exercises first. Preserve their original
    names, and pass the selected exercise_catalog_id or null if unmatched. Set
    remember_alias=true only on request. Keep weights in kg exactly as reported
    and use null only for explicitly unweighted sets. Never infer equipment
    from weights or double dumbbell weights. After an uncertain write, check
    get_workout_sessions before retrying.
    """
    if confirmed is not True:
        raise ValueError("Show the workout changes to the user and get confirmation before saving")
    if workout_type is not None and (not isinstance(workout_type, str) or (workout_type != "" and workout_type not in _workout_types)):
        raise ValueError("Choose a supported workout type")
    if workout_focus is not None:
        if not isinstance(workout_focus, str):
            raise ValueError("workout_focus must be text")
        workout_focus = workout_focus.strip()
        if len(workout_focus) > 120 or "\0" in workout_focus:
            raise ValueError("Enter a workout focus of up to 120 characters")
        if workout_type is None or (workout_type == "" and workout_focus):
            raise ValueError("Provide workout_type when setting workout_focus")
    if exercises is None:
        exercises = []
    if not isinstance(exercises, list) or len(exercises) > 20 or (not exercises and workout_type is None):
        raise ValueError("Provide between 1 and 20 exercises")
    if visit_id is not None and (type(visit_id) is not int or visit_id <= 0):
        raise ValueError("Choose a visit ID returned by get_workout_sessions")
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
    target, visits = _workout_visits(workout_date)
    if not visits:
        raise ValueError(f"No gym visit is recorded for {target}")
    if visit_id is None:
        if len(visits) > 1:
            raise ValueError(f"Multiple gym visits are recorded for {target}; choose a specific visit")
        visit_id = visits[0]["id"]
    elif all(visit["id"] != visit_id for visit in visits):
        raise ValueError(f"The selected gym visit is not recorded for {target}")
    body = {"exercises": exercises}
    if workout_type is not None:
        body.update(workout_type=workout_type, workout_focus=workout_focus or "")
    saved = _request(f"/api/gym-visits/{visit_id}/exercises/batch", method="POST", body=body)
    if workout_type is None:
        return json.dumps(saved)
    return json.dumps({
        "workout_date": target, "visit_id": visit_id, "workout_type": workout_type or None,
        "workout_focus": workout_focus or None, "exercises": saved,
    })


class MaintenanceActionInput(TypedDict):
    component: str
    action: str
    description: str


def _vehicle_id(vehicle_id):
    if type(vehicle_id) is not int or vehicle_id <= 0:
        raise ValueError("Choose a vehicle ID returned by get_vehicles")
    return vehicle_id


@mcp.tool()
def get_vehicle_maintenance(vehicle_id: int, component: str = "", action: str = "") -> str:
    """Read vehicle maintenance history and upcoming estimates. Choose the vehicle with get_vehicles. For component history pass a lowercase underscore key, e.g. engine_oil, clutch_cable, brake_cable, front_brake_pads. Filter action=replaced to answer last changed/replaced; adjusted, inspected or topped_up do not mean replacement. Results are newest service date first, up to 50 matching services. Return the first qualifying record for 'last', with date, odometer and source record/attachment. An empty result means no recorded match, not that work never happened. Without a component returns all recorded services and coming_up. General service reminders require general_service/completed. My intervals start prefilled and are editable; manual values are entered by the user."""
    import re
    _vehicle_id(vehicle_id)
    if component:
        if not re.fullmatch(r"[a-z][a-z0-9_]{0,63}", component) or (action and action not in _maintenance_actions):
            raise ValueError("Provide a component key and supported action")
        result = _get(f"/api/vehicles/{vehicle_id}/maintenance-history?{urlencode({'component': component, 'action': action})}")
        if not isinstance(result, list):
            raise ValueError("Invalid maintenance history from PULSE")
        return json.dumps({"records": result, "limit": 50, "order": "service_date_desc"})
    if action:
        raise ValueError("A component is required when filtering an action")
    result = _get(f"/api/vehicles/{vehicle_id}/history")
    if not isinstance(result, dict) or not isinstance(result.get("maintenance_records"), list):
        raise ValueError("Invalid vehicle history from PULSE")
    return json.dumps({"records": result["maintenance_records"], "coming_up": result.get("coming_up", []), "maintenance_schedules": result.get("maintenance_schedules", [])})


_maintenance_actions = {"replaced", "inspected", "adjusted", "repaired", "topped_up", "cleaned", "lubricated", "completed"}


@mcp.tool()
def record_vehicle_maintenance(vehicle_id: int, title: str, service_date: str, amount: float,
                               actions: list[MaintenanceActionInput], category: str = "service",
                               odometer_km: float | None = None, provider_name: str = "",
                               notes: str = "", confirmed: bool = False) -> str:
    """Save manually reported completed work after explicit confirmation. First show the exact vehicle, service date, mileage, amount, and every component/action to the user. Ask about ambiguous invoice wording; never treat requested work or a pre-invoice as proof of completion. service_date is required YYYY-MM-DD in India time; never infer it from an upload date. Amount may be zero for free/DIY work. Actions use component keys (engine_oil, oil_filter, clutch_cable, etc.) and replaced/inspected/adjusted/repaired/topped_up/cleaned/lubricated/completed. Oil change is engine_oil/replaced; full service is general_service/completed. Preserve original wording in description. This tool does not upload or extract invoices. After an uncertain write/network failure inspect existing history before retrying: a repeated write can create duplicates."""
    import math
    import re
    if confirmed is not True:
        raise ValueError("Show the complete maintenance entry and obtain explicit confirmation before saving")
    _vehicle_id(vehicle_id)
    try:
        parsed_date = date.fromisoformat(service_date)
    except (ValueError, TypeError):
        raise ValueError("Provide the service date as YYYY-MM-DD") from None
    if parsed_date.isoformat() != service_date or parsed_date > datetime.now(_india_time).date():
        raise ValueError("Provide a service date no later than today")
    if not isinstance(title, str) or not title.strip() or len(title.strip().encode()) > 160:
        raise ValueError("A title of at most 160 bytes is required")
    if category not in {"service", "repair", "insurance", "washing", "tyres"}:
        raise ValueError("Unsupported maintenance category")
    if type(amount) not in (int, float) or not math.isfinite(amount) or not 0 <= amount < 1e10:
        raise ValueError("Provide a non-negative finite amount below 10 billion")
    if odometer_km is not None and (type(odometer_km) not in (int, float) or not math.isfinite(odometer_km) or not 0 <= odometer_km < 1e11):
        raise ValueError("Provide a non-negative finite odometer")
    if not isinstance(provider_name, str) or len(provider_name.strip().encode()) > 160 or not isinstance(notes, str):
        raise ValueError("Provide a provider of at most 160 bytes and text notes")
    if not isinstance(actions, list) or len(actions) > 100:
        raise ValueError("Provide at most 100 maintenance actions")
    seen = set()
    normalized = []
    for item in actions:
        if not isinstance(item, dict):
            raise ValueError("Each maintenance action must be an object")
        component, action, description = item.get("component"), item.get("action"), item.get("description", "")
        if not isinstance(component, str) or not re.fullmatch(r"[a-z][a-z0-9_]{0,63}", component) or not isinstance(action, str) or action not in _maintenance_actions:
            raise ValueError("Each action needs a component key and supported action")
        if not isinstance(description, str) or len(description.encode()) > 500 or (component, action) in seen:
            raise ValueError("Descriptions must be at most 500 bytes; duplicate component/action pairs are not allowed")
        seen.add((component, action))
        normalized.append({"component": component, "action": action, "description": description})
    occurred_at = datetime.combine(parsed_date, datetime.min.time(), _india_time).isoformat()
    result = _request(f"/api/vehicles/{vehicle_id}/maintenance-records", "POST", {
        "category": category, "title": title.strip(), "occurred_at": occurred_at, "amount": amount,
        "odometer_km": odometer_km, "provider_name": provider_name.strip() or None,
        "notes": notes.strip() or None, "actions": normalized,
    })
    return json.dumps(result)


class ReminderIntervalInput(TypedDict):
    kilometers: int
    duration: int


@mcp.tool()
def configure_vehicle_reminder(vehicle_id: int, kind: str, active_source: str,
                               personal: ReminderIntervalInput, manual: ReminderIntervalInput,
                               manual_reference: str = "", confirmed: bool = False) -> str:
    """Save both reminder schedules and choose which is active after explicit confirmation. First read get_vehicle_maintenance without filters, retain both existing personal/manual values, and show the complete proposed settings. kind is engine_oil, general_service or tire_pressure. active_source is personal or manual; only that source applies, never a mixture. Each interval has nonnegative integer kilometers and duration in calendar months; zero disables a threshold. At least one threshold is required for the active personal/manual schedule. Either active threshold being reached makes work due. Manual values are user-entered, not independently verified official guidance; preserve their manual_reference. Inactive values remain saved. These settings affect Coming up; only existing tire-pressure notifications are automatically delivered, not new oil/service push notifications."""
    if confirmed is not True:
        raise ValueError("Show both saved schedules and obtain explicit confirmation before changing reminders")
    _vehicle_id(vehicle_id)
    if kind not in {"engine_oil", "general_service", "tire_pressure"} or active_source not in {"personal", "manual"}:
        raise ValueError("Choose a supported task and active schedule")
    for interval in (personal, manual):
        if not isinstance(interval, dict):
            raise ValueError("Provide both complete interval objects")
        km, duration, unit = interval.get("kilometers"), interval.get("duration"), interval.get("unit", "months")
        if type(km) is not int or type(duration) is not int or unit != "months" or not 0 <= km <= 1000000 or not 0 <= duration <= 1200:
            raise ValueError("Provide nonnegative whole intervals; time is always in months")
    selected = personal if active_source == "personal" else manual
    if not (selected["kilometers"] or selected["duration"]):
        raise ValueError("The active schedule needs a distance or time interval")
    if not isinstance(manual_reference, str) or len(manual_reference.encode()) > 160:
        raise ValueError("Manual reference must be at most 160 bytes")
    return json.dumps(_request(f"/api/vehicles/{vehicle_id}/maintenance-schedules/{kind}", "PUT", {
        "active_source": active_source, "personal": dict(personal, unit="months"), "manual": dict(manual, unit="months"),
        "manual_reference": manual_reference,
    }))


if __name__ == "__main__":
    mcp.run(transport="streamable-http", host="0.0.0.0", port=8000,
            stateless_http=True, json_response=True)
