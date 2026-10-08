import importlib.util
from datetime import timedelta
from pathlib import Path
import unittest
from unittest.mock import patch
import json


spec = importlib.util.spec_from_file_location("export_seed", Path(__file__).with_name("export-dev-seed.py"))
seed = importlib.util.module_from_spec(spec)
spec.loader.exec_module(seed)


class SeedExportTest(unittest.TestCase):
    def test_personal_values_are_replaced_and_unknown_numbers_fail_closed(self):
        row = {"id": "7", "user_id": "1", "_seed_ordinal": "3", "raw_value": "37"}
        transform = lambda table, column, kind, value: seed.anonymize(
            table, column, kind, value, row, timedelta(days=-1), "7")
        self.assertEqual(transform("users", "username", "character varying", "private-user"), "admin")
        self.assertEqual(transform("users", "password_hash", "character varying", "private-hash"), seed.PASSWORD_HASH)
        self.assertEqual(transform("fitness_workouts", "metadata", "jsonb", '{"email":"private@example.org"}'), "{}")
        self.assertEqual(transform("financial_horizon_transactions", "notes", "text", "private note"), "Demo notes 7")
        self.assertEqual(transform("fitness_workouts", "start_time", "timestamp with time zone", "2026-10-08T17:09:44.43138+05:30"), "2026-10-07 08:00:00+05:30")
        self.assertNotEqual(transform("fitness_workouts", "uuid", "uuid", "private-uuid"), "private-uuid")
        self.assertEqual(transform("gym_visits", "user_id", "bigint", "1"), "1")
        self.assertEqual(transform("exercise_aliases", "normalized_alias", "text", "private alias"), "demo alias 3")
        self.assertEqual(transform("vehicles", "name", "character varying", r"\N"), r"\N")
        with self.assertRaises(ValueError):
            transform("vehicles", "new_numeric_field", "bigint", "123")

    def test_export_has_atomic_empty_database_and_environment_guards(self):
        columns = [{"table_name": table, "column_name": "id", "data_type": "bigint"}
                   for table in sorted(seed.TABLES)]
        columns += [{"table_name": "users", "column_name": name, "data_type": "character varying"}
                    for name in ("username", "password_hash")]
        records = {table: [] for table in seed.TABLES - seed.EXCLUDED}
        records["users"] = [{"id": 1, "username": "private-user", "password_hash": "private-hash"}]
        answers = [json.dumps(columns), "23", "1", "2026-10-08", "[]", json.dumps(records)]
        with patch.object(seed, "query", side_effect=answers):
            sql, counts = seed.export({})
        self.assertEqual(counts["users"], 1)
        self.assertNotIn("private-user", sql)
        self.assertNotIn("private-hash", sql)
        for table in seed.EXCLUDED:
            self.assertNotIn(f"COPY public.{table} ", sql)
        self.assertIn("BEGIN;", sql)
        self.assertIn("COMMIT;", sql)
        self.assertIn("LOCK TABLE", sql)
        self.assertIn(r"\if :seed_empty", sql)
        self.assertIn("seed_environment", sql)
        self.assertIn("version = 23 AND NOT dirty", sql)
        self.assertIn("setval(pg_get_serial_sequence", sql)

    def test_schema_changes_require_review(self):
        with patch.object(seed, "query", return_value='[{"table_name":"unexpected","column_name":"id","data_type":"bigint"}]'):
            with self.assertRaisesRegex(ValueError, "Schema tables changed"):
                seed.export({})


if __name__ == "__main__":
    unittest.main()
