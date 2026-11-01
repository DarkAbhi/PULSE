//go:build integration

package db_test

import (
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestPersonalOwnershipUpgrade(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	if err := db.RunMigrationSteps(dsn, -2); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO meditations DEFAULT VALUES;
        INSERT INTO credit_cards (name,link) VALUES ('Existing card','https://example.com');
        INSERT INTO sports (name) VALUES ('cricket')`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrations(dsn); err != nil {
		t.Fatal(err)
	}
	for _, table := range []string{"meditations", "credit_cards", "sports"} {
		var count int
		if err := database.QueryRow(`SELECT count(*) FROM ` + table + ` WHERE user_id=1`).Scan(&count); err != nil {
			t.Fatal(err)
		}
		if count != 1 {
			t.Fatalf("%s backfill count = %d", table, count)
		}
	}
	if _, err := database.Exec(`INSERT INTO meditations DEFAULT VALUES`); err == nil {
		t.Fatal("ownerless insert accepted")
	}
}

func TestAmbiguousPersonalOwnershipStopsAndCanBeMapped(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	if err := db.RunMigrationSteps(dsn, -2); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO users (username,password_hash) VALUES ('second','test');
        INSERT INTO sports (name) VALUES ('football')`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrations(dsn); err == nil {
		t.Fatal("ambiguous historical owner accepted")
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM sports WHERE name='football'`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("historical rows lost: %d, %v", count, err)
	}
	// The failed migration is transactional. Simulate the documented reviewed mapping and dirty-state repair.
	if _, err := database.Exec(`ALTER TABLE sports ADD COLUMN user_id BIGINT;
        UPDATE sports SET user_id=(SELECT id FROM users WHERE username='second');
        UPDATE schema_migrations SET version=28,dirty=false`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrations(dsn); err != nil {
		t.Fatal(err)
	}
	if err := database.QueryRow(`SELECT count(*) FROM sports s JOIN users u ON s.user_id=u.id WHERE u.username='second'`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("explicit mapping lost: %d, %v", count, err)
	}
}

func TestRelationshipsCannotCrossOwners(t *testing.T) {
	database, stop := testhelper.StartPostgres(t)
	defer stop()
	if _, err := database.Exec(`INSERT INTO users (id,username,password_hash) VALUES (2,'second','test');
        INSERT INTO vehicles (id,user_id,name,fuel_types) VALUES (100,1,'First vehicle',ARRAY['petrol']);
        INSERT INTO financial_horizon_budgets (id,user_id,name,allocated_amount) VALUES (100,1,'Private budget',500);
        INSERT INTO financial_horizon_subscriptions (id,user_id,name,amount) VALUES (100,1,'Private subscription',20);
        INSERT INTO notifications (id,user_id,source,title) VALUES (100,1,'test','Private notification')`); err != nil {
		t.Fatal(err)
	}
	for _, statement := range []string{
		`INSERT INTO vehicle_air_fills (vehicle_id,user_id) VALUES (100,2)`,
		`INSERT INTO vehicle_fuel_fillups (vehicle_id,user_id,odometer_km) VALUES (100,2,0)`,
		`INSERT INTO vehicle_maintenance_records (vehicle_id,user_id,category,title,amount) VALUES (100,2,'service','Invalid',1)`,
		`INSERT INTO financial_horizon_deductions (user_id,name,amount,budget_id) VALUES (2,'Invalid',1,100)`,
		`INSERT INTO financial_horizon_transactions (user_id,name,amount,budget_id) VALUES (2,'Invalid',1,100)`,
		`INSERT INTO financial_horizon_transactions (user_id,name,amount,subscription_id) VALUES (2,'Invalid',1,100)`,
		`INSERT INTO meal_times (name,start_time,end_time) VALUES ('Invalid shared','08:00','09:00')`,
	} {
		if _, err := database.Exec(statement); err == nil {
			t.Fatalf("invalid relationship accepted: %s", statement)
		}
	}
	if _, err := database.Exec(`INSERT INTO financial_horizon_transactions (user_id,name,amount,budget_id,subscription_id) VALUES (1,'Valid',1,100,100);
        DELETE FROM financial_horizon_budgets WHERE id=100;
        DELETE FROM financial_horizon_subscriptions WHERE id=100;`); err != nil {
		t.Fatal(err)
	}
	var unlinked bool
	if err := database.QueryRow(`SELECT user_id=1 AND budget_id IS NULL AND subscription_id IS NULL FROM financial_horizon_transactions WHERE name='Valid'`).Scan(&unlinked); err != nil || !unlinked {
		t.Fatalf("SET NULL semantics changed: %v, %v", unlinked, err)
	}
	if _, err := database.Exec(`INSERT INTO vehicle_air_fills (vehicle_id,user_id) VALUES (100,1); DELETE FROM vehicles WHERE id=100`); err != nil {
		t.Fatal(err)
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM vehicle_air_fills WHERE vehicle_id=100`).Scan(&count); err != nil || count != 0 {
		t.Fatalf("cascade changed: %d, %v", count, err)
	}
}
