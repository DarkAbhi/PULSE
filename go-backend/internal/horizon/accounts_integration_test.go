//go:build integration

package horizon

import (
	"encoding/json"
	"fmt"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestAccountDetailsMigrationAndLifecycle(t *testing.T) {
	t.Chdir("../..")
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	if err := db.RunMigrationSteps(dsn, -1); err != nil {
		t.Fatal(err)
	}
	var legacyID int64
	if err := database.QueryRow(`INSERT INTO financial_accounts(user_id,display_name,institution_name,account_type,currency,last_four)
 VALUES (1,'Legacy bank','Kotak','bank_account','INR','4321') RETURNING id`).Scan(&legacyID); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO financial_horizon_transactions(user_id,name,amount,account_id) VALUES (1,'History',10,$1)`, legacyID); err != nil {
		t.Fatal(err)
	}
	if _, err := database.Exec(`INSERT INTO financial_accounts(user_id,display_name,institution_name,account_type,currency,last_four,archived_at)
 VALUES (1,'Legacy card','HDFC','credit_card','INR','0123',now())`); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}
	var links, bankRows, cardRows, removedColumns int
	if err := database.QueryRow(`SELECT
 (SELECT count(*) FROM financial_horizon_transactions WHERE account_id=$1),
 (SELECT count(*) FROM bank_accounts WHERE account_id=$1 AND last_four='4321'),
 (SELECT count(*) FROM credit_cards WHERE institution_name='HDFC' AND last_four='0123'),
 (SELECT count(*) FROM information_schema.columns WHERE table_name IN ('financial_accounts','bank_accounts','credit_cards') AND column_name IN ('currency','country_code'))`, legacyID).Scan(&links, &bankRows, &cardRows, &removedColumns); err != nil || links != 0 || bankRows != 0 || cardRows != 0 || removedColumns != 0 {
		t.Fatalf("development account reset: links=%d banks=%d cards=%d removed=%d err=%v", links, bankRows, cardRows, removedColumns, err)
	}
	// Empty account schema supports rollback and reapplication.
	if err := db.RunMigrationSteps(dsn, -1); err != nil {
		t.Fatal(err)
	}
	if err := db.RunMigrationSteps(dsn, 1); err != nil {
		t.Fatal(err)
	}

	pool := testPool(t, dsn)
	defer pool.Close()
	router := chi.NewRouter()
	NewHandler(pool, NewService(pool), testSessionLookup(database)).RegisterRoutes(router)
	cookie := loginUser(t, database)
	request := func(method, path, body string, want int) FinancialAccountDTO {
		t.Helper()
		req := httptest.NewRequest(method, path, strings.NewReader(body))
		req.AddCookie(cookie)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != want {
			t.Fatalf("%s %s: %d %s", method, path, rec.Code, rec.Body.String())
		}
		var out FinancialAccountDTO
		if want == 200 || want == 201 {
			if err := json.Unmarshal(rec.Body.Bytes(), &out); err != nil {
				t.Fatal(err)
			}
		}
		return out
	}
	bank := `{"display_name":"Legacy bank","institution_name":"Kotak","account_type":"bank_account","bank_account_subtype":"savings","last_four":"4321","holder_name":"A Person","holder_email":"person@example.com","account_number":"00004321"}`
	saved := request("POST", "/horizon/accounts", bank, 201)
	legacyID = saved.ID
	if _, err := database.Exec(`INSERT INTO financial_horizon_transactions(user_id,name,amount,account_id) VALUES (1,'Linked history',10,$1)`, legacyID); err != nil {
		t.Fatal(err)
	}
	var preserved int
	if err := database.QueryRow(`SELECT count(*) FROM financial_horizon_transactions WHERE name='History' AND amount=10 AND account_id IS NULL`).Scan(&preserved); err != nil || preserved != 1 {
		t.Fatalf("transaction history lost: %d %v", preserved, err)
	}
	if saved.AccountNumber != "00004321" || saved.HolderEmail != "person@example.com" {
		t.Fatalf("bank round trip: %+v", saved)
	}
	request("PUT", fmt.Sprintf("/horizon/accounts/%d", legacyID), strings.Replace(bank, "00004321", "11114321", 1), 409)
	request("PUT", fmt.Sprintf("/horizon/accounts/%d", legacyID), strings.Replace(bank, "A Person", "Updated Holder", 1), 200)
	card := `{"display_name":"Rewards","institution_name":"HDFC","account_type":"credit_card","holder_name":"A Person","card_type":"Millennia","card_network":"rupay","last_four":"0123","credit_limit":200000.5,"statement_day":31}`
	saved = request("POST", "/horizon/accounts", card, 201)
	if saved.CardType != "Millennia" || saved.CardNetwork != "rupay" || saved.CreditLimit == nil || *saved.CreditLimit != 200000.5 || saved.StatementDay == nil || *saved.StatementDay != 31 {
		t.Fatalf("card round trip: %+v", saved)
	}
	path := fmt.Sprintf("/horizon/accounts/%d", saved.ID)
	request("PUT", path, strings.Replace(card, "200000.5", "300000", 1), 200)
	archived := request("POST", path+"/archive", "", 200)
	if archived.ArchivedAt == nil {
		t.Fatal("not archived")
	}
	restored := request("POST", path+"/restore", "", 200)
	if restored.ArchivedAt != nil {
		t.Fatal("not restored")
	}
	// Changing an unused card to a bank removes the card detail row atomically.
	request("PUT", path, bank, 200)
	var leftovers int
	if err := database.QueryRow(`SELECT count(*) FROM credit_cards WHERE account_id=$1`, saved.ID).Scan(&leftovers); err != nil || leftovers != 0 {
		t.Fatalf("orphan card details: %d %v", leftovers, err)
	}
	var foreignID int64
	if err := database.QueryRow(`WITH owner AS (INSERT INTO users(username,password_hash) VALUES ('account-foreign','unused') RETURNING id)
 INSERT INTO financial_accounts(user_id,display_name,account_type) SELECT id,'Private','bank_account' FROM owner RETURNING id`).Scan(&foreignID); err != nil {
		t.Fatal(err)
	}
	request("PUT", fmt.Sprintf("/horizon/accounts/%d", foreignID), bank, 404)
	request("POST", fmt.Sprintf("/horizon/accounts/%d/archive", foreignID), "", 404)
	request("POST", "/horizon/accounts", strings.Replace(card, `"statement_day":31`, `"statement_day":32`, 1), 400)
	request("POST", "/horizon/accounts", strings.Replace(card, `"last_four":"0123"`, `"last_four":"1234567890123456"`, 1), 400)
}
