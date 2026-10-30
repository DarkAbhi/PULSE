"""Seed restore checks using newly created disposable PostgreSQL databases."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from urllib.parse import urlsplit, urlunsplit
import uuid


ROOT = Path(__file__).resolve().parent.parent
DATABASE_URL = os.environ.get("TEST_SEED_DATABASE_URL")


@unittest.skipUnless(DATABASE_URL, "Set TEST_SEED_DATABASE_URL to a disposable PostgreSQL server")
class SeedDatabaseTest(unittest.TestCase):
    def run_psql(self, *args, url=None, check=True):
        result = subprocess.run(["psql", "-XqAt", "-v", "ON_ERROR_STOP=1",
                                 "--dbname", url or self.url, *args],
                                text=True, capture_output=True)
        if check and result.returncode:
            self.fail(f"psql exited with status {result.returncode}\n"
                      f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}")
        return result

    def setUp(self):
        self.name = "pulse_seed_test_" + uuid.uuid4().hex
        parts = urlsplit(DATABASE_URL)
        self.url = urlunsplit(parts._replace(path="/" + self.name))
        self.run_psql("-c", f"CREATE DATABASE {self.name}", url=DATABASE_URL)
        self.addCleanup(self.run_psql, "-c", f"DROP DATABASE {self.name}", url=DATABASE_URL)
        migrations = sorted((ROOT / "go-backend/migrations").glob("*.up.sql"))
        args = [argument for path in migrations for argument in ("-f", str(path))]
        version = int(migrations[-1].name.split("_")[0])
        self.run_psql("-1", *args, "-c", "CREATE TABLE schema_migrations (version bigint, dirty boolean); "
                      f"INSERT INTO schema_migrations VALUES ({version}, false)")

    def restore(self, environment="test", check=True, path=None):
        return self.run_psql("-v", f"seed_environment={environment}", "-f",
                             str(path or ROOT / "go-backend/data/dev-seed.sql"), check=check)

    def test_fresh_restore_repeated_restore_and_sequences(self):
        self.assertIn("Applied anonymized", self.restore().stdout)
        self.assertEqual(self.run_psql("-c", "SELECT timezone, currency FROM user_profiles WHERE user_id = 1").stdout.strip(), "UTC|INR")
        count = self.run_psql("-c", "SELECT count(*) FROM fitness_activity_rings").stdout
        self.assertGreater(int(count), 0)
        self.assertEqual(self.run_psql("-c", "SELECT max(created_at)::date = CURRENT_DATE FROM gym_visits").stdout.strip(), "t")
        self.assertIn("skipped", self.restore().stdout)
        self.assertEqual(count, self.run_psql("-c", "SELECT count(*) FROM fitness_activity_rings").stdout)
        self.assertEqual(self.run_psql("-c", "SELECT count(*) FROM api_keys UNION ALL SELECT count(*) FROM user_sessions UNION ALL SELECT count(*) FROM vehicle_maintenance_attachments").stdout.split(), ["0", "0", "0"])
        self.run_psql("-c", "INSERT INTO vehicles (name, user_id, vehicle_type, fuel_types) VALUES ('Sequence check', 1, 'two_wheeler', ARRAY['petrol'])")

    def test_populated_database_is_untouched(self):
        self.run_psql("-c", "INSERT INTO vehicles (name, user_id, vehicle_type, fuel_types) VALUES ('Existing development vehicle', 1, 'two_wheeler', ARRAY['petrol'])")
        self.assertIn("skipped", self.restore().stdout)
        self.assertEqual(self.run_psql("-c", "SELECT name FROM vehicles").stdout.strip(), "Existing development vehicle")
        self.assertEqual(self.run_psql("-c", "SELECT count(*) FROM fitness_activity_rings").stdout.strip(), "0")

    def test_environment_and_schema_mismatch_are_rejected(self):
        rejected = self.restore("production", check=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("Development seed requires seed_environment", rejected.stderr)
        self.run_psql("-c", "UPDATE schema_migrations SET version = 999")
        rejected = self.restore(check=False)
        self.assertNotEqual(rejected.returncode, 0)
        self.assertIn("Seed schema version mismatch", rejected.stderr)
        self.assertEqual(self.run_psql("-c", "SELECT count(*) FROM user_profiles").stdout.strip(), "0")

    def test_restore_error_rolls_back_partial_data(self):
        sql = (ROOT / "go-backend/data/dev-seed.sql").read_text()
        sql = sql.replace(r"\echo Applied anonymized development seed.", "SELECT 1 / 0;")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "broken-seed.sql"
            path.write_text(sql)
            failed = self.restore(check=False, path=path)
            self.assertNotEqual(failed.returncode, 0)
            self.assertIn("division by zero", failed.stderr)
        self.assertEqual(self.run_psql("-c", "SELECT count(*) FROM fitness_activity_rings").stdout.strip(), "0")
        self.assertEqual(self.run_psql("-c", "SELECT username FROM users").stdout.strip(), "admin")
        self.run_psql("-c", "INSERT INTO users (username, password_hash) VALUES ('Sequence rollback check', 'test')")


if __name__ == "__main__":
    unittest.main()
