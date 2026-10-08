"""Export a shareable, anonymized data-only seed from the development database."""

import argparse
from datetime import date, datetime, timedelta
import json
import os
from pathlib import Path
import re
from graphlib import TopologicalSorter
import subprocess
import uuid


ROOT = Path(__file__).resolve().parent.parent
PASSWORD_HASH = "$2y$12$bB7WwVq7nGJ4cfNTCX6kQODcNRLQvMjRhIFuH4Qv2.GAxlqNac4/S"
EXCLUDED = {"schema_migrations", "exercise_catalog", "api_keys", "user_sessions",
            "vehicle_maintenance_attachments"}
TABLES = set("""api_keys credit_cards exercise_aliases exercise_catalog
financial_horizon_budgets financial_horizon_categories financial_horizon_configs
financial_horizon_deductions financial_horizon_subscriptions financial_horizon_transactions
fitness_activity_rings fitness_workouts gym_exercise_sets gym_visit_exercises gym_visits
meal_plans meal_times meditations next_month_purchases notifications schema_migrations
sports users user_profiles user_sessions vehicle_air_fills vehicle_fuel_fillups vehicle_fuel_items
vehicle_maintenance_attachments vehicle_maintenance_records vehicles workout_activity_types""".split())
BASELINE = {"users", "exercise_catalog", "schema_migrations",
            "financial_horizon_categories", "meal_times"}
ENUMS = {
    "billing_cycle": {"monthly", "yearly"}, "status": {"active", "paused", "cancelled"},
    "type": {"debit", "credit"}, "fuel_type": {"petrol", "diesel", "lpg", "cng", "electric"},
    "fill_type": {"full", "partial", "missed"},
}
NUMBERS = {
    "amount": 500, "allocated_amount": 5000, "base_amount": 50000, "price": 1500,
    "quantity": 5, "unit_price": 100, "total_cost": 500, "odometer_km": 1000,
    "weight": 10, "reps": 10, "duration_seconds": 1800, "calories_burned": 200,
    "distance_meters": 5000, "move_calories": 200, "move_calories_goal": 350,
    "exercise_minutes": 30, "exercise_minutes_goal": 40, "stand_hours": 8,
    "stand_hours_goal": 12, "steps_count": 6000,
}


def environment(path):
    env = os.environ.copy()
    fields = dict(DB_HOSTNAME="PGHOST", DB_PORT="PGPORT", DB_USERNAME="PGUSER",
                  DB_PASSWORD="PGPASSWORD", DB_NAME="PGDATABASE", DB_SSLMODE="PGSSLMODE")
    values = {}
    for line in path.read_text().splitlines():
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
                value = value[1:-1]
            values[key.strip()] = value
    for source, target in fields.items():
        if values.get(source):
            env[target] = values[source]
    if not all(env.get(key) for key in ("PGHOST", "PGUSER", "PGDATABASE")):
        raise ValueError("Development database host, user, and name must be configured")
    env["PGCONNECT_TIMEOUT"] = "10"
    return env


def query(sql, env):
    return subprocess.check_output(["psql", "-XAt", "-v", "ON_ERROR_STOP=1", "-c", sql],
                                   env=env, text=True).strip()


def anonymize(table, column, kind, value, row, offset, admin_id):
    if value == r"\N":
        return value
    row_id = row.get("id", row.get("user_id", "1"))
    number = int(row_id) if row_id.isdigit() else 1
    if column == "password_hash":
        return PASSWORD_HASH
    if column == "username":
        return "admin" if row_id == admin_id else f"demo{row_id}"
    if column == "uuid":
        return str(uuid.uuid5(uuid.NAMESPACE_URL, f"pulse-demo/{table}/{row_id}"))
    if kind in ("date", "timestamp with time zone", "timestamp without time zone"):
        value = re.sub(r"\.\d+", lambda m: m[0].ljust(7, "0"), value)
        original = datetime.fromisoformat(value) if kind != "date" else date.fromisoformat(value)
        shifted = original + offset
        if isinstance(shifted, datetime):
            shifted = shifted.replace(hour=8, minute=30 if column == "end_time" else 0,
                                      second=0, microsecond=0)
            return shifted.isoformat(sep=" ")
        return shifted.isoformat()
    if kind == "time without time zone":
        return "08:00:00" if column == "start_time" else "09:00:00"
    if kind == "boolean":
        return value
    if column == "id" or column.endswith("_id"):
        if kind in ("bigint", "integer", "smallint") or column == "exercise_catalog_id":
            return value
    if column in ("raw_value", "set_number", "priority"):
        return value
    if column in ("due_day", "billing_day"):
        return "15"
    if column in NUMBERS:
        return str(NUMBERS[column] + (number % 5 if column in ("amount", "weight") else 0))
    if column.startswith(("front_tire_pressure", "rear_tire_pressure")):
        return "32"
    if kind == "jsonb":
        return "{}"
    if column == "currency":
        return "INR"
    if column in ENUMS:
        if value not in ENUMS[column]:
            raise ValueError(f"Unknown enum in {table}.{column}; review anonymization rules")
        return value
    if table == "sports" and column == "name":
        if value not in {"cricket", "football", "badminton"}:
            raise ValueError("Unknown sport")
        return value
    if table == "vehicle_maintenance_records" and column == "category":
        if value not in {"service", "repair", "insurance", "washing", "tyres"}:
            raise ValueError("Unknown maintenance category")
        return value
    if table == "workout_activity_types" and column == "name":
        names = {"13": "Cycling", "37": "Running", "50": "Traditional Strength Training",
                 "20": "Functional Strength Training", "52": "Walking"}
        return names.get(row["raw_value"], f"Demo workout {row_id}")
    if column == "icon":
        return "tag"
    if column == "color":
        return "#64748b"
    if column in ("link", "url"):
        return "https://example.com"
    if column == "target_path":
        return "/dashboard"
    if column == "normalized_alias":
        return f"demo alias {row['_seed_ordinal']}"
    if column == "category_name":
        return f"Demo name {row['category_id']}" if row.get("category_id", r"\N") != r"\N" else "Other"
    if kind in ("text", "character varying", "character"):
        return f"Demo {column.replace('_', ' ')} {row_id}"
    raise ValueError(f"No anonymization rule for {table}.{column} ({kind})")


def export(env):
    columns = json.loads(query("""SELECT json_agg(c) FROM (
        SELECT table_name, column_name, data_type FROM information_schema.columns
        WHERE table_schema = 'public' ORDER BY table_name, ordinal_position) c""", env))
    kinds = {(c["table_name"], c["column_name"]): c["data_type"] for c in columns}
    if {c["table_name"] for c in columns} != TABLES:
        raise ValueError("Schema tables changed; review the seed exporter before sharing data")
    version = int(query("SELECT version FROM schema_migrations WHERE NOT dirty", env))
    admin_id = query("SELECT min(id) FROM users", env)
    if not admin_id:
        raise ValueError("The source database needs at least one user")
    latest = query("SELECT coalesce(max(created_at)::date, CURRENT_DATE) FROM gym_visits", env)
    offset = date(2026, 1, 31) - date.fromisoformat(latest)
    dependencies = json.loads(query("""SELECT coalesce(json_agg(d), '[]') FROM (
        SELECT conrelid::regclass::text AS child, confrelid::regclass::text AS parent
        FROM pg_constraint WHERE contype = 'f'
        AND connamespace = 'public'::regnamespace) d""", env))
    graph = {table: set() for table in sorted(TABLES - EXCLUDED)}
    for dependency in dependencies:
        child, parent = dependency["child"], dependency["parent"]
        if child in graph and parent in graph:
            graph[child].add(parent)
    ordered = list(TopologicalSorter(graph).static_order())
    parts = [f"'{table}', (SELECT coalesce(json_agg(t ORDER BY to_jsonb(t)::text), '[]') FROM public.{table} t)"
             for table in ordered]
    # One SQL statement gives all tables a consistent snapshot; raw rows stay in memory.
    records = json.loads(query("SELECT json_build_object(" + ", ".join(parts) + ")", env))
    data, counts = [], {}
    for table in ordered:
        names = [c["column_name"] for c in columns if c["table_name"] == table]
        data.append(f"COPY public.{table} ({', '.join(names)}) FROM stdin;")
        counts[table] = len(records[table])
        for ordinal, record in enumerate(records[table], 1):
            row = {key: r"\N" if value is None else str(value).lower() if isinstance(value, bool)
                   else str(value) for key, value in record.items()}
            row["_seed_ordinal"] = str(ordinal)
            sanitized = [anonymize(table, name, kinds[table, name], row[name],
                                   row, offset, admin_id) for name in names]
            data.append("\t".join(value if value == r"\N" else
                                  value.replace("\\", "\\\\").replace("\t", r"\t")
                                  .replace("\n", r"\n").replace("\r", r"\r")
                                  for value in sanitized))
        data.append(r"\.")
        if "id" in names:
            data.append(f"SELECT setval(pg_get_serial_sequence('public.{table}', 'id'), "
                        f"coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.{table};")
    if not counts["users"]:
        raise ValueError("Incomplete source snapshot")
    tables = ", ".join(f"public.{table}" for table in sorted(TABLES - {"schema_migrations"}))
    empty = " AND ".join(f"NOT EXISTS (SELECT FROM public.{table})"
                         for table in sorted(TABLES - BASELINE))
    empty += (" AND NOT EXISTS (SELECT FROM users WHERE username <> 'admin' OR password_hash <> "
              f"'{PASSWORD_HASH}') AND (SELECT count(*) FROM users) = 1"
              " AND NOT EXISTS (SELECT FROM financial_horizon_categories WHERE user_id IS NOT NULL)"
              " AND NOT EXISTS (SELECT FROM meal_times WHERE user_id IS NOT NULL)")
    truncate = ", ".join(f"public.{table}" for table in sorted(TABLES - {"schema_migrations", "exercise_catalog"}))
    header = f"""-- Anonymized development snapshot; regenerate with make dev-seed-export.
-- Data values are fictional. Relationships and relative dates follow the source.
\\set ON_ERROR_STOP on
SELECT :'seed_environment' IN ('development', 'test') AS allowed \\gset
\\if :allowed
\\else
DO $$ BEGIN RAISE EXCEPTION 'Development seed requires seed_environment=development or test'; END $$;
\\endif
BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE {tables} IN SHARE ROW EXCLUSIVE MODE;
SELECT {empty} AS seed_empty \\gset
\\if :seed_empty
\\else
\\echo Database already populated; development seed skipped.
COMMIT;
\\quit
\\endif
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM schema_migrations WHERE version = {version} AND NOT dirty) THEN
        RAISE EXCEPTION 'Seed schema version mismatch; regenerate the development seed';
    END IF;
END $$;
TRUNCATE {truncate} RESTART IDENTITY;
"""
    # Ensure the browser account has a profile even if none existed in the snapshot.
    profile = (f"INSERT INTO user_profiles (user_id, display_name) VALUES ({admin_id}, 'Demo Admin') "
               "ON CONFLICT (user_id) DO NOTHING;")
    updates = []
    for table in ordered:
        assignments = []
        for c in columns:
            if c["table_name"] != table:
                continue
            name, kind = c["column_name"], c["data_type"]
            if name == "target_month":
                assignments.append(f"{name} = date_trunc('month', CURRENT_DATE + interval '1 month')::date")
            elif kind == "date":
                assignments.append(f"{name} = {name} + (CURRENT_DATE - DATE '2026-01-31')")
            elif kind.startswith("timestamp"):
                assignments.append(f"{name} = {name} + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day'")
        if assignments:
            updates.append(f"UPDATE public.{table} SET {', '.join(assignments)};")
    footer = f"""
{chr(10).join(updates)}
{profile}
\\echo Applied anonymized development seed.
COMMIT;
"""
    return header + "\n".join(data) + footer, counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=ROOT / ".env.dev")
    args = parser.parse_args()
    seed, counts = export(environment(args.env_file))
    output = ROOT / "go-backend" / "data" / "dev-seed.sql"
    output.write_text(seed)
    print(f"Wrote anonymized seed: {sum(counts.values())} records across {len(counts)} tables")


if __name__ == "__main__":
    main()
