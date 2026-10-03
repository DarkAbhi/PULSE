import io
import json
import os
import sys
import types
import unittest
from urllib.error import HTTPError
from unittest.mock import patch


class LifeTrackerMCPTest(unittest.TestCase):
    def test_tools_use_fixed_endpoints_and_require_write_confirmation(self):
        class FakeMCP:
            def __init__(self, _name):
                self.tools = []

            def tool(self):
                def register(fn):
                    self.tools.append(fn.__name__)
                    return fn
                return register

        mcp_server = types.ModuleType("mcp.server")
        mcp_server.MCPServer = FakeMCP
        with patch.dict(sys.modules, {"mcp": types.ModuleType("mcp"),
                                      "mcp.server": mcp_server}):
            import importlib.util
            from pathlib import Path
            spec = importlib.util.spec_from_file_location("life_tracker_mcp", Path(__file__).with_name("server.py"))
            server = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(server)

        self.assertEqual(server.mcp.tools, ["get_workout_today", "get_vehicles", "search_exercises", "add_exercises_to_workout"])
        calls = []
        login_count = 0

        def get(request, timeout):
            nonlocal login_count
            url = request.full_url
            token = request.get_header("Authorization")
            calls.append((url, request.get_method(), token, timeout))
            if url.endswith("/auth/login"):
                self.assertEqual(json.loads(request.data), {"username": "me", "password": "secret"})
                login_count += 1
                response = io.BytesIO(b'{"username":"me"}')
                response.headers = {"Set-Cookie": f"life_session=token{login_count}; HttpOnly"}
                return response
            if url.endswith("/vehicles") and token == "Bearer token1":
                raise HTTPError(url, 401, "expired", {}, None)
            if url.endswith("/exercises/batch"):
                self.assertEqual(request.get_method(), "POST")
                self.assertEqual(json.loads(request.data), {"exercises": workout})
                return io.BytesIO(json.dumps(workout).encode())
            value = {"visited": True, "id": 12} if url.endswith("/workout/today") else [{"id": 7, "name": "Scooter"}]
            return io.BytesIO(json.dumps(value).encode())

        workout = [{"name": "Bicep curls", "exercise_catalog_id": None, "sets": [{"reps": 10, "weight": 12.5}]},
                   {"name": "Barbell squats", "exercise_catalog_id": "Barbell_Full_Squat", "sets": [{"reps": 8, "weight": 50}]}]
        with patch.dict(os.environ, {"MCP_BACKEND_USERNAME": "me", "MCP_BACKEND_PASSWORD": "secret"}), \
             patch.object(server, "urlopen", side_effect=get):
            self.assertTrue(json.loads(server.get_workout_today())["visited"])
            self.assertEqual(json.loads(server.get_vehicles())[0]["name"], "Scooter")
            with self.assertRaisesRegex(ValueError, "confirmation"):
                server.add_exercises_to_workout(workout)
            with self.assertRaisesRegex(ValueError, "rep count"):
                server.add_exercises_to_workout([{"name": "Curl", "exercise_catalog_id": None, "sets": [{"reps": 0, "weight": 10}]}], True)
            self.assertEqual(json.loads(server.add_exercises_to_workout(workout, True)), workout)

        self.assertEqual(calls, [
            ("http://127.0.0.1:18080/api/auth/login", "POST", None, 10),
            ("http://127.0.0.1:18080/api/workout/today", "GET", "Bearer token1", 10),
            ("http://127.0.0.1:18080/api/vehicles", "GET", "Bearer token1", 10),
            ("http://127.0.0.1:18080/api/auth/login", "POST", None, 10),
            ("http://127.0.0.1:18080/api/vehicles", "GET", "Bearer token2", 10),
            ("http://127.0.0.1:18080/api/workout/today", "GET", "Bearer token2", 10),
            ("http://127.0.0.1:18080/api/gym-visits/12/exercises/batch", "POST", "Bearer token2", 10),
        ])

        result = {"query": "weight plate front raises", "match_type": "suggested",
                  "exercise_catalog_id": None,
                  "candidates": [{"id": "Front_Plate_Raise", "name": "Front Plate Raise"}]}
        with patch.object(server, "_get", return_value=result) as lookup:
            self.assertEqual(json.loads(server.search_exercises(["weight plate front raises"])), [result])
            lookup.assert_called_once_with("/api/exercise-catalog?q=weight+plate+front+raises")
            with self.assertRaisesRegex(ValueError, "exercise names"):
                server.search_exercises([])
            with self.assertRaisesRegex(ValueError, "100 characters"):
                server.search_exercises([" "])
        with patch.object(server, "_request") as write:
            with self.assertRaisesRegex(ValueError, "Search exercises first"):
                server.add_exercises_to_workout([{"name": "Curl", "sets": [{"reps": 10, "weight": 5}]}], True)
            with self.assertRaisesRegex(ValueError, "Remembering an alias"):
                server.add_exercises_to_workout([dict(workout[0], remember_alias=True)], True)
            write.assert_not_called()

        visits = [
            {"id": 29, "created_at": "2026-09-28T20:00:00Z"},
            {"id": 30, "created_at": "2026-09-29T20:00:00Z"},
        ]
        with patch.object(server, "_get", return_value=visits) as lookup, \
             patch.object(server, "_request", return_value=workout) as write:
            self.assertEqual(json.loads(server.add_exercises_to_workout(workout, True, "2026-09-29")), workout)
            lookup.assert_called_once_with("/api/gym-visits")
            write.assert_called_once_with("/api/gym-visits/29/exercises/batch",
                                          method="POST", body={"exercises": workout})
            with self.assertRaisesRegex(ValueError, "No gym visit"):
                server.add_exercises_to_workout(workout, True, "2026-09-27")
            with self.assertRaisesRegex(ValueError, "YYYY-MM-DD"):
                server.add_exercises_to_workout(workout, True, "2026-09-31")
            with self.assertRaisesRegex(ValueError, "Multiple gym visits"):
                lookup.return_value = visits + [{"id": 31, "created_at": "2026-09-29T10:00:00+05:30"}]
                server.add_exercises_to_workout(workout, True, "2026-09-29")
            self.assertEqual(write.call_count, 1)

        server._session_token = None
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "MCP_BACKEND_USERNAME"):
                server.get_workout_today()


if __name__ == "__main__":
    unittest.main()
