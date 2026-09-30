import io
import json
import os
import sys
import types
import unittest
from urllib.error import HTTPError
from unittest.mock import patch


class LifeTrackerMCPTest(unittest.TestCase):
    def test_read_tools_call_only_their_fixed_get_endpoints(self):
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

        self.assertEqual(server.mcp.tools, ["get_workout_today", "get_vehicles"])
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
            value = {"visited": True, "id": 12} if url.endswith("/workout/today") else [{"id": 7, "name": "Scooter"}]
            return io.BytesIO(json.dumps(value).encode())

        with patch.dict(os.environ, {"MCP_BACKEND_USERNAME": "me", "MCP_BACKEND_PASSWORD": "secret"}), \
             patch.object(server, "urlopen", side_effect=get):
            self.assertTrue(json.loads(server.get_workout_today())["visited"])
            self.assertEqual(json.loads(server.get_vehicles())[0]["name"], "Scooter")

        self.assertEqual(calls, [
            ("http://127.0.0.1:18080/api/auth/login", "POST", None, 10),
            ("http://127.0.0.1:18080/api/workout/today", "GET", "Bearer token1", 10),
            ("http://127.0.0.1:18080/api/vehicles", "GET", "Bearer token1", 10),
            ("http://127.0.0.1:18080/api/auth/login", "POST", None, 10),
            ("http://127.0.0.1:18080/api/vehicles", "GET", "Bearer token2", 10),
        ])

        server._session_token = None
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "MCP_BACKEND_USERNAME"):
                server.get_workout_today()


if __name__ == "__main__":
    unittest.main()
