package statementimport

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgtype"
)

func syntheticDocument(first string, second string) Document {
	var page Page
	put := func(top float64, cells ...any) {
		for i := 0; i < len(cells); i += 2 {
			x := float64(cells[i].(int))
			text := cells[i+1].(string)
			page.Words = append(page.Words, Word{Text: text, X0: x, X1: x + 3, Top: top})
		}
	}
	put(20, 35, "Account Statement")
	put(30, 35, "Kotak Mahindra Bank")
	put(40, 35, "Account No. 0000004321")
	put(50, 35, "Savings Account")
	put(60, 35, "Currency INDIAN RUPEE")
	put(100, 44, "#", 75, "Date", 124, "Description", 280, "Chq/Ref.", 355, "Withdrawal", 429, "Deposit", 498, "Balance")
	put(120, 39, "-", 72, "-", 118, "Opening Balance", 532, "1,000.00")
	put(140, 39, "1", 72, "02 Jan 2026", 118, "Synthetic market", 391, first, 525, "980.00")
	put(148, 118, "wrap line")
	firstPage := page
	page = Page{}
	put(100, 51, "#", 82, "Date", 131, "Description", 287, "Chq/Ref.", 362, "Withdrawal", 436, "Deposit", 505, "Balance")
	put(120, 46, "2", 79, "04 Jan 2026", 125, "Synthetic refund", 464, second, 532, "985.00")
	put(200, 35, "Particulars Opening Balance Closing Balance")
	put(219, 44, "Savings Account", 365, "1,000.00", 498, "985.00")
	return Document{PageCount: 2, Pages: []Page{firstPage, page}}
}

func TestSyntheticBankProfile(t *testing.T) {
	doc := syntheticDocument("20.00", "5.00")
	ledger, fail := parseDocument(doc)
	if fail != nil {
		t.Fatalf("parse failed with %s", fail.code)
	}
	if ledger.Profile.ID != "kotak_savings_v1" || ledger.LastFour != "4321" || len(ledger.Rows) != 2 {
		t.Fatal("wrong profile, account identifier or row count")
	}
	if ledger.Rows[0].Description != "Synthetic market wrap line" || ledger.Rows[0].Amount != 2000 ||
		ledger.Rows[1].Date != "2026-01-04" || ledger.Rows[1].Type != "credit" {
		t.Fatal("incorrect geometry or debit/credit mapping")
	}
	tests := []struct {
		name string
		doc  Document
		fail *failure
	}{
		{"broken running balance", syntheticDocument("21.00", "5.00"), reconciliationFailed},
		{"invalid precision", syntheticDocument("20.000", "5.00"), invalidRow},
		{"unrecognized", Document{}, unsupported},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, got := parseDocument(tt.doc)
			if got != tt.fail {
				t.Fatalf("unexpected rejection: %v", got)
			}
		})
	}
}

func TestSBIReconcilesWithOverlaidOpeningLabel(t *testing.T) {
	for _, tt := range []struct {
		name    string
		opening string
		fail    *failure
	}{
		{"reconciled", "1,000.00", nil},
		{"missing opening amount", "", balancesUnavailable},
		{"incorrect opening amount", "1,001.00", reconciliationFailed},
	} {
		t.Run(tt.name, func(t *testing.T) {
			var page Page
			put := func(top float64, x float64, text string) {
				page.Words = append(page.Words, Word{Top: top, X0: x, X1: x + 2, Text: text})
			}
			put(10, 20, "State Bank of India")
			put(20, 20, "SAVING ACCOUNT INR")
			put(30, 20, "Account Number: 0000004321")
			put(40, 24, "Date")
			put(40, 126, "Transaction Reference")
			put(40, 297, "Ref.No./Chq.No.")
			put(40, 397, "Credit")
			put(40, 468, "Debit")
			put(40, 532, "Balance")
			put(55, 12, "Yourn Oupllening")
			put(55, 62, "nBualllance on 01-01-26:")
			if tt.opening != "" {
				put(55, 184, tt.opening)
			}
			for _, x := range []float64{358, 428, 498, 568} {
				put(55, x, "null")
			}
			put(70, 19, "02-01-26")
			put(70, 62, "Synthetic deposit")
			put(70, 405, "25.00")
			put(70, 505, "0")
			put(70, 545, "1,025.00")
			put(85, 12, "Your Closing Balance on 31-01-26: 1,025.00")

			ledger, fail := parseDocument(Document{PageCount: 1, Pages: []Page{page}})
			if fail != tt.fail {
				t.Fatalf("unexpected outcome: %v", fail)
			}
			if fail == nil && (ledger.Profile.ID != "sbi_savings_v1" || ledger.Opening != 100000 ||
				ledger.Closing != 102500 || len(ledger.Rows) != 1) {
				t.Fatal("wrong SBI profile, balances, or transaction count")
			}
		})
	}
}

func TestParseMoney(t *testing.T) {
	p := profile{DecimalSep: ".", GroupSep: ","}
	checks := map[string]int64{"0.00": 0, "1,23,456.78": 12345678, "123,456.78": 12345678, "-40.01": -4001}
	for value, want := range checks {
		if got, ok := p.money(value); !ok || got != want {
			t.Fatalf("incorrect amount for %q", value)
		}
	}
	for _, value := range []string{"12,34.00", "1.000", "5", ".30", "1e5.00", "100,000,000,000,000,000.00"} {
		if _, ok := p.money(value); ok {
			t.Fatalf("invalid amount %q was accepted", value)
		}
	}
}

func TestSourceFingerprint(t *testing.T) {
	a := LedgerRow{Date: "2026-01-01", Type: "credit", Amount: 500, Balance: 5000, Description: " TEST   Store "}
	b := a
	b.Description = "test store"
	if sourceFingerprint(a) != sourceFingerprint(b) {
		t.Fatal("normalization changed the fingerprint")
	}
	b.Balance += 500
	if sourceFingerprint(a) == sourceFingerprint(b) {
		t.Fatal("different running balances must remain distinct")
	}
}

func TestUploadRouteAndPDFValidation(t *testing.T) {
	r := chi.NewRouter()
	r.Route("/api", func(r chi.Router) {
		NewHandler(nil, func(*http.Request) (int64, error) { return 0, sql.ErrNoRows }, "").RegisterRoutes(r)
	})
	res := httptest.NewRecorder()
	r.ServeHTTP(res, httptest.NewRequest("POST", "/api/horizon/statement-imports", nil))
	if res.Code != 401 {
		t.Fatalf("expected authenticated route, got %d", res.Code)
	}

	for _, tt := range []struct {
		file string
		fail *failure
	}{{"%PDF-1.7", nil}, {"a different file", notPDF}, {"%PDF-" + strings.Repeat("x", maxPDFSize), tooLarge}} {
		var body bytes.Buffer
		writer := multipart.NewWriter(&body)
		_ = writer.WriteField("account_id", "1")
		part, err := writer.CreateFormFile("file", "ignored.pdf")
		if err != nil {
			t.Fatal(err)
		}
		_, _ = part.Write([]byte(tt.file))
		_ = writer.Close()
		req := httptest.NewRequest("POST", "/api/horizon/statement-imports", &body)
		req.Header.Set("Content-Type", writer.FormDataContentType())
		pdf, _, failure := readPDF(httptest.NewRecorder(), req)
		if failure != tt.fail || (failure == nil && string(pdf) != tt.file) {
			t.Fatal("unexpected PDF validation result")
		}
	}
}

func TestCategoryEditExplicitNullClearsSelection(t *testing.T) {
	id := int64(7)
	row := rowPreview{Description: "Example", CategoryID: &id}
	if fail := applyEdit(&row, editableRow{CategoryID: json.RawMessage("null")}); fail != nil ||
		row.CategoryID != nil || !row.CategoryChanged {
		t.Fatal("explicit category clear was not applied")
	}
}

func TestHDFCRoundedDueRequiresExactTransactionTotals(t *testing.T) {
	makeDocument := func(debit, due string) Document {
		var page Page
		add := func(top float64, x float64, text string) {
			page.Words = append(page.Words, Word{Top: top, X0: x, X1: x + 2, Text: text})
		}
		add(10, 20, "Millennia Credit Card Statement")
		add(20, 20, "HDFC Bank")
		add(30, 20, "Credit Card No. 000000XXXXXX1234")
		add(40, 20, "Previous Statement Dues Total Amount Due")
		for i, amount := range []string{"C100.26", "C5.13", debit, "C0.00", due} {
			add(55, 20+100*float64(i), amount)
		}
		add(80, 20, "Domestic Transactions")
		add(90, 20, "DATE & TIME")
		add(90, 150, "TRANSACTION DESCRIPTION")
		add(90, 525, "AMOUNT PI")
		add(105, 20, "02/01/2026| 12:42")
		add(105, 150, "Synthetic purchase")
		add(105, 505, "C 10.12 l")
		add(120, 20, "03/01/2026| 09:01")
		add(120, 150, "Synthetic refund")
		add(120, 505, "+ C 5.13 l")
		return Document{PageCount: 1, Pages: []Page{page}}
	}
	ledger, fail := parseDocument(makeDocument("C10.12", "C105.00"))
	if fail != nil || ledger.Profile.ID != "hdfc_millennia_credit_v1" || ledger.LastFour != "1234" ||
		len(ledger.Rows) != 2 || ledger.Rows[1].Type != "credit" || ledger.Closing != 10525 {
		t.Fatalf("valid rounded summary failed: %v", fail)
	}
	for _, doc := range []Document{makeDocument("C10.13", "C105.00"), makeDocument("C10.12", "C106.00")} {
		if _, fail := parseDocument(doc); fail != reconciliationFailed {
			t.Fatalf("mismatched statement was not rejected: %v", fail)
		}
	}
}

func TestCardReviewPreservesStatementTotals(t *testing.T) {
	ledger := []LedgerRow{
		{Date: "2026-01-03", Type: "debit", Amount: 1200},
		{Date: "2026-01-01", Type: "credit", Amount: 500},
	}
	preview := []rowPreview{
		{Date: "2026-01-03", Type: "debit", Amount: "12.00", Included: true},
		{Date: "2026-01-01", Type: "credit", Amount: "5.00", Included: false},
	}
	batch := query.StatementImportBatch{
		ProfileID:    "icici_amazon_credit_v1",
		OpeningMinor: pgtype.Int8{Int64: 9_000, Valid: true},
		ClosingMinor: pgtype.Int8{Int64: 9_700, Valid: true},
	}
	if _, fail := verifyPreview(batch, ledger, preview); fail != nil {
		t.Fatalf("unchanged card totals failed with %s", fail.code)
	}
	preview[0].Amount = "12.01"
	preview[1].Amount = "5.01"
	for _, profileID := range []string{"icici_amazon_credit_v1", "hdfc_millennia_credit_v1"} {
		batch.ProfileID = profileID
		if _, fail := verifyPreview(batch, ledger, preview); fail != reconciliationFailed {
			t.Fatal("offsetting edits bypassed printed debit and credit totals")
		}
	}
}

func TestIDFCMarkedBalance(t *testing.T) {
	p := profile{DecimalSep: ".", GroupSep: ","}
	for _, tt := range []struct {
		input string
		want  int64
	}{
		{"1,001.22 CR", 100122},
		{"1,001.22 DR", -100122},
	} {
		if got, ok := p.markedBalance(tt.input); !ok || got != tt.want {
			t.Fatalf("incorrect marked balance for synthetic input %q", tt.input)
		}
	}
	if _, ok := p.markedBalance("1,001.22"); ok {
		t.Fatal("accepted balance without a debit or credit marker")
	}
}
