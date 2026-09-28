import io
import json
import sys
import types
import unittest
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
        paths = []

        def get(url, timeout):
            paths.append((url, timeout))
            value = {"visited": True, "id": 12} if url.endswith("/workout/today") else [{"id": 7, "name": "Scooter"}]
            return io.BytesIO(json.dumps(value).encode())

        with patch.object(server, "urlopen", side_effect=get):
            self.assertTrue(json.loads(server.get_workout_today())["visited"])
            self.assertEqual(json.loads(server.get_vehicles())[0]["name"], "Scooter")

        self.assertEqual(paths, [
            ("http://127.0.0.1:18080/api/workout/today", 10),
            ("http://127.0.0.1:18080/api/vehicles", 10),
        ])


if __name__ == "__main__":
    unittest.main()
