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

func TestVehicleCatalogUpgradeAndRepair(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	// The disposable database starts at the current version. Return to the
	// pre-catalog schema and insert a vehicle exactly as an old installation did.
	if err := db.RollbackMigration(dsn); err != nil {
		t.Fatal(err)
	}
	if err := db.RollbackMigration(dsn); err != nil {
		t.Fatal(err)
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
