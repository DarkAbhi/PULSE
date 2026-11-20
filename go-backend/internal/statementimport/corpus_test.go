package statementimport

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"unicode/utf8"

	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
	"github.com/jackc/pgx/v5/pgtype"
)

// Optional local smoke test for redacted outcomes from an extracted private corpus.
func TestPrivateStatementGeometry(t *testing.T) {
	dir := os.Getenv("PULSE_STATEMENT_GEOMETRY_DIR")
	if dir == "" {
		t.Skip("no private document geometry supplied")
	}
	files, err := filepath.Glob(filepath.Join(dir, "*.json"))
	if err != nil || len(files) == 0 {
		t.Fatal("no extracted data")
	}
	expectedRows := map[string]int{
		"kotak_savings_v1": 22, "idfc_first_savings_v1": 10, "icici_amazon_credit_v1": 5,
		"hdfc_millennia_credit_v1": 23, "hdfc_swiggy_credit_v1": 13, "axis_my_zone_credit_v1": 2,
		"federal_savings_v1": 8, "idfc_first_credit_v1": 2, "sbi_savings_v1": 19,
	}
	for i, file := range files {
		b, err := os.ReadFile(file)
		if err != nil {
			t.Fatal("could not read extracted data")
		}
		var d Document
		if err := json.Unmarshal(b, &d); err != nil {
			t.Fatal("could not decode extracted data")
		}
		l, fail := parseDocument(d)
		if fail != nil {
			t.Errorf("sample %d: %s, %d rows", i+1, fail.code, len(l.Rows))
		} else {
			if want := expectedRows[l.Profile.ID]; want == 0 || want != len(l.Rows) {
				t.Errorf("sample %d: unexpected row count", i+1)
			}
			batch := query.StatementImportBatch{
				ProfileID: l.Profile.ID, OpeningMinor: pgtype.Int8{Int64: l.Opening, Valid: true},
				ClosingMinor: pgtype.Int8{Int64: l.Closing, Valid: true},
			}
			preview := make([]rowPreview, len(l.Rows))
			categorized, renamed := 0, 0
			fuelOutletFound := false
			for rowIndex, row := range l.Rows {
				suggestion := suggestNarration(row)
				compactNarration := strings.ToUpper(strings.Join(strings.Fields(row.Description), ""))
				if strings.Contains(compactNarration, "KRJFUELSTATIONMYSORE") {
					fuelOutletFound = true
					if suggestion.primary != "Transport" || suggestion.child != "fuel" {
						t.Errorf("sample %d: fuel outlet must select Transport / fuel", i+1)
					}
				}
				if suggestion.primary != "" {
					categorized++
				}
				if suggestion.name != row.Description {
					renamed++
				}
				if suggestion.name == "" || !utf8.ValidString(suggestion.name) ||
					utf8.RuneCountInString(suggestion.name) > 255 {
					t.Errorf("sample %d: invalid suggested name for row %d", i+1, rowIndex+1)
				}
				preview[rowIndex] = rowPreview{
					Date: row.Date, Amount: decimal(row.Amount), Type: row.Type,
					Description: suggestion.name, OriginalNarration: row.Description,
				}
			}
			if l.Profile.ID == "hdfc_millennia_credit_v1" && !fuelOutletFound {
				t.Errorf("sample %d: expected fuel outlet entry was not extracted", i+1)
			}
			if _, fail := verifyPreview(batch, l.Rows, preview); fail != nil {
				t.Errorf("sample %d: confirmation validation failed with %s", i+1, fail.code)
			}
			preview[0].Amount = decimal(l.Rows[0].Amount + 1)
			if _, fail := verifyPreview(batch, l.Rows, preview); fail != reconciliationFailed {
				t.Errorf("sample %d: altered amount was not rejected by reconciliation", i+1)
			}
			t.Logf("sample %d: %s, %d rows reconciled, %d category suggestions, %d names cleaned",
				i+1, l.Profile.ID, len(l.Rows), categorized, renamed)
		}
	}
}
