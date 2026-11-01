//go:build integration

package db_test

import (
	"context"
	"strings"
	"testing"

	"github.com/DarkAbhi/life-backend/data/vehiclesdb"
	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestRetiredPersonalTablesUpgradeAndRollback(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	if err := db.RollbackMigration(dsn); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO meditations (user_id) VALUES (1);
        INSERT INTO credit_cards (user_id,name,link) VALUES (1,'Old card','https://example.com');
        INSERT INTO sports (user_id,name) VALUES (1,'cricket');
        INSERT INTO vehicles (user_id,name,fuel_types) VALUES (1,'Keep vehicle',ARRAY['petrol'])`); err != nil {
		t.Fatal(err)
	}
	for _, step := range []int{1, -1, 1} {
		if err := db.RunMigrationSteps(dsn, step); err != nil {
			t.Fatal(err)
		}
		for _, table := range []string{"meditations", "credit_cards", "sports"} {
			var exists bool
			if err := database.QueryRow(`SELECT to_regclass($1) IS NOT NULL`, "public."+table).Scan(&exists); err != nil {
				t.Fatal(err)
			}
			if exists != (step == -1) {
				t.Fatalf("%s exists=%v after step %d", table, exists, step)
			}
			if exists {
				var count int
				if err := database.QueryRow(`SELECT count(*) FROM ` + table).Scan(&count); err != nil || count != 0 {
					t.Fatalf("%s rollback should restore an empty table: count=%d, error=%v", table, count, err)
				}
			}
		}
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM vehicles WHERE name='Keep vehicle' AND user_id=1`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("unrelated vehicle lost: count=%d, error=%v", count, err)
	}
}

func TestVehicleCatalogUpgradeAndRepair(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	// The disposable database starts at the current version. Return to the
	// pre-catalog schema and insert a vehicle exactly as an old installation did.
	for {
		var version int
		if err := database.QueryRow(`SELECT version FROM schema_migrations`).Scan(&version); err != nil {
			t.Fatal(err)
		}
		if version == 26 {
			break
		}
		if err := db.RollbackMigration(dsn); err != nil {
			t.Fatal(err)
		}
	}
	if _, err := database.Exec(`INSERT INTO vehicles (name,user_id,vehicle_type,fuel_types)
        VALUES ('Existing nickname',1,'four_wheeler',ARRAY['diesel'])`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrations(dsn); err != nil {
		t.Fatal(err)
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM vehicle_catalog`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 15068 {
		t.Fatalf("catalog count: %d", count)
	}
	var name, kind, fuel string
	if err := database.QueryRow(`SELECT name,kind,fuel_types[1] FROM vehicles`).Scan(&name, &kind, &fuel); err != nil {
		t.Fatal(err)
	}
	if name != "Existing nickname" || kind != "unknown" || fuel != "diesel" {
		t.Fatalf("existing data changed: %s %s %s", name, kind, fuel)
	}
	if _, err := database.Exec(`DELETE FROM vehicle_catalog WHERE id='car/honda/civic'`); err != nil {
		t.Fatal(err)
	}
	// A late failure must roll back earlier successful inserts in the same seed.
	connection, err := database.Conn(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	brokenSeed := strings.Replace(vehiclesdb.Seed, "COMMIT;", "SELECT 1 / 0; COMMIT;", 1)
	_, importErr := connection.ExecContext(context.Background(), brokenSeed)
	_, rollbackErr := connection.ExecContext(context.Background(), "ROLLBACK")
	closeErr := connection.Close()
	if importErr == nil || rollbackErr != nil || closeErr != nil {
		t.Fatalf("failed import cleanup: import=%v rollback=%v close=%v", importErr, rollbackErr, closeErr)
	}
	if err := database.QueryRow(`SELECT count(*) FROM vehicle_catalog WHERE id='car/honda/civic'`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 0 {
		t.Fatal("failed import committed partial reference data")
	}
	for i := 0; i < 2; i++ {
		if err := db.RunMigrations(dsn); err != nil {
			t.Fatal(err)
		}
	}
	if err := database.QueryRow(`SELECT count(*) FROM vehicle_catalog`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 15068 {
		t.Fatalf("repeat/repair count: %d", count)
	}
	if err := database.QueryRow(`SELECT count(*) FROM vehicle_catalog_imports WHERE version='2026.10.4' AND length(sha256)=64`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 1 {
		t.Fatalf("invalid import provenance: %d", count)
	}
}
