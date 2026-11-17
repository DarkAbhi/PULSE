package statementimport

import (
	"database/sql"
	"encoding/json"
	"strings"
	"testing"
	"unicode/utf8"

	horizonquery "github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
)

func TestSuggestNarration(t *testing.T) {
	t.Parallel()
	for _, tt := range []struct {
		name, narration, direction, description, primary string
	}{
		{"wrapped brokerage", "UPI/DR/123456789012/ICCL\nZER/HDFC/zerodha/z8s7a6sd78asd", "debit", "Zerodha payment", "Investment"},
		{"grocery handle", "UPI-DR-7654321-SWIGGY-instamart@upi-7654321", "debit", "Swiggy Instamart", "Groceries"},
		{"ordinary UPI debit", "UPI/DR/123456789012/Asha Rao/SBIN/asha@upi/Zomato-looking-id", "debit", "UPI payment to Asha Rao", ""},
		{"ordinary UPI credit", "UPI/CR/123456789012/Asha Rao/SBIN/asha@upi", "credit", "UPI received from Asha Rao", ""},
		{"labeled UPI refund", "UPI/CR/123456789012/SWIGGY/HDFC/swiggy@upi/REFUND", "credit", "Refund", "Return"},
		{"opaque UPI fields", "UPI/DR/123456789012/XXXX1234/548923423/xy983sz1pke2p", "debit", "UPI payment", ""},
		{"bank code without payee", "UPI/DR/123456789012/HDFC/9876543210@ybl", "debit", "UPI payment", ""},
		{"wrapped plain text", "Acme\n Market (RRN: 987654321)", "debit", "Acme Market", ""},
		{"IMPS payee", "IMPS/P2A/123456789012/ASHA RAO/SBIN0001234", "debit", "IMPS payment to ASHA RAO", ""},
		{"IMPS bank code", "IMPS/000123/HDFC-ASHA RAO/987654321", "debit", "IMPS payment to ASHA RAO", ""},
		{"NEFT payee", "NEFT-HDFC0001234-SAMPLE COMPANY-987654321", "credit", "NEFT received from SAMPLE COMPANY", ""},
		{"online NEFT", "INB NEFT/123456789012/SAMPLE COMPANY", "debit", "NEFT payment to SAMPLE COMPANY", ""},
		{"RTGS payment", "RTGS/123456789012/OFFICE RENT", "debit", "Business rent", "Business"},
		{"card purchase", "POS/123456789012/SWIGGY/BENGALURU", "debit", "Swiggy", "Food and Drinks"},
		{"ecommerce purchase", "ECOM/PUR/123456789012/NETFLIX", "debit", "Netflix", "Subscription"},
		{"direct debit", "NACH/123456789012/INSURANCE PREMIUM", "debit", "Insurance premium", "Insurance"},
		{"bank charge", "BANK SERVICE CHARGE", "debit", "Bank fee", "Hidden Charges"},
		{"empty narration", "\t\u0000", "debit", "Statement transaction", ""},
		{"international text length limit", strings.Repeat("界", 256), "debit", strings.Repeat("界", 255), ""},
	} {
		t.Run(tt.name, func(t *testing.T) {
			got := suggestNarration(LedgerRow{Description: tt.narration, Type: tt.direction})
			if got.name != tt.description || got.primary != tt.primary || !utf8.ValidString(got.name) {
				t.Errorf("got %q / %q, want %q / %q", got.name, got.primary, tt.description, tt.primary)
			}
		})
	}
}

func TestOriginalNarrationSurvivesReview(t *testing.T) {
	t.Parallel()
	source := LedgerRow{Description: "UPI/DR/123456789012/SWIGGY/HDFC/swiggy@upi", Type: "debit"}
	raw, err := json.Marshal(source)
	if err != nil {
		t.Fatal(err)
	}
	preview, _, err := rowsForReview([]query.StatementImportRow{{
		Source: raw, Preview: []byte(`{"description":"Dinner","original_narration":"changed"}`),
	}})
	if err != nil {
		t.Fatal(err)
	}
	if preview[0].Description != "Dinner" || preview[0].OriginalNarration != source.Description {
		t.Fatal("the editable name and original source narration must remain separate")
	}
}

func TestSuggestionLearningScope(t *testing.T) {
	t.Parallel()
	key := func(description, direction string) string {
		t.Helper()
		return suggestNarration(LedgerRow{Description: description, Type: direction}).key
	}
	purchase := key("UPI/DR/12345678/SWIGGY/HDFC/swiggy@upi", "debit")
	if purchase == "" || purchase != key("SWIGGY* BENGALURU", "debit") {
		t.Fatal("recognized UPI and card purchases must share a preference")
	}
	for _, other := range []string{
		key("UPI/DR/99999999/SWIGGY INSTAMART/HDFC/swiggy@upi", "debit"),
		key("SWIGGY REFUND", "credit"),
		key("HDFC SWIGGY CREDIT CARD PAYMENT", "debit"),
	} {
		if other == purchase {
			t.Fatal("different payment contexts must not share a preference")
		}
	}
	if key("UPI/DR/12345678/654321", "debit") != "" {
		t.Fatal("a UPI reference alone must not produce a merchant preference")
	}
	if key("UPI/DR/12345678/ASHA/asha@okhdfcbank", "debit") ==
		key("UPI/DR/98765432/SOMEONE ELSE/asha@ybl", "debit") {
		t.Fatal("different UPI addresses must not share a preference")
	}
	if key("UPI/DR/12345678/ASHA/9876543210@ybl", "debit") ==
		key("UPI/DR/98765432/ASHA/9876543211@ybl", "debit") {
		t.Fatal("numeric UPI addresses must also distinguish recipients")
	}
}

func TestSuggestedCategoryID(t *testing.T) {
	t.Parallel()
	categories := []horizonquery.ListCategoriesRow{
		{ID: 10, Name: "Food and Drinks", IsDefault: true},
		{ID: 20, Name: "Groceries", IsDefault: true},
		{ID: 30, Name: "Savings", IsDefault: true},
		{ID: 11, Name: "others", IsDefault: true, ParentID: sql.NullInt64{Int64: 10, Valid: true}},
		{ID: 12, Name: "Swiggy", IsDefault: true, ParentID: sql.NullInt64{Int64: 10, Valid: true}},
		{ID: 21, Name: "others", IsDefault: true, ParentID: sql.NullInt64{Int64: 20, Valid: true}},
		{ID: 40, Name: "Food and Drinks", UserID: sql.NullInt64{Int64: 99, Valid: true}},
	}
	paths := defaultCategoryPaths(categories)
	for _, tt := range []struct {
		primary, child string
		want           int64
	}{
		{"Food and Drinks", "Swiggy", 12},
		{"Food and Drinks", "missing child", 11},
		{"Groceries", "Swiggy", 21},
		{"Groceries", "", 21},
		{"Savings", "", 30},
		{"Unknown", "", 0},
		{"", "", 0},
	} {
		t.Run(tt.primary+"/"+tt.child, func(t *testing.T) {
			got := suggestedCategoryID(paths, categorySuggestion{primary: tt.primary, child: tt.child})
			if (tt.want == 0 && got != nil) || (tt.want != 0 && (got == nil || *got != tt.want)) {
				t.Errorf("category ID = %v, want %d (0 means no match)", got, tt.want)
			}
		})
	}
}
