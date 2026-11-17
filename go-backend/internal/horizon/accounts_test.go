package horizon

import (
	"math"
	"strings"
	"testing"
)

func TestNormalizeFinancialAccount(t *testing.T) {
	valid := FinancialAccountInput{DisplayName: " Savings ", InstitutionName: " Example Bank ", AccountType: "bank_account", BankAccountSubtype: "savings", AccountNumber: "001234", HolderName: " A Person ", HolderEmail: " person@example.com "}
	got, err := normalizeFinancialAccount(valid)
	if err != nil || got.DisplayName != "Savings" || got.HolderName != "A Person" || got.HolderEmail != "person@example.com" || got.AccountNumber != "001234" || got.LastFour != "1234" {
		t.Fatalf("valid bank account not normalized: %+v, %v", got, err)
	}
	for name, change := range map[string]func(*FinancialAccountInput){
		"empty name":           func(v *FinancialAccountInput) { v.DisplayName = " " },
		"long name":            func(v *FinancialAccountInput) { v.DisplayName = strings.Repeat("x", 101) },
		"control character":    func(v *FinancialAccountInput) { v.InstitutionName = "Bank\nName" },
		"invalid type":         func(v *FinancialAccountInput) { v.AccountType = "debit_card" },
		"invalid subtype":      func(v *FinancialAccountInput) { v.BankAccountSubtype = "other" },
		"empty holder":         func(v *FinancialAccountInput) { v.HolderName = " " },
		"invalid email":        func(v *FinancialAccountInput) { v.HolderEmail = "invalid" },
		"display email":        func(v *FinancialAccountInput) { v.HolderEmail = "Person <person@example.com>" },
		"long number":          func(v *FinancialAccountInput) { v.AccountNumber = strings.Repeat("1", 35) },
		"invalid number":       func(v *FinancialAccountInput) { v.AccountNumber = "12 34" },
		"card details on bank": func(v *FinancialAccountInput) { v.CardType = "Millennia" },
	} {
		t.Run(name, func(t *testing.T) {
			in := valid
			change(&in)
			if _, err := normalizeFinancialAccount(in); err == nil {
				t.Fatal("invalid bank account accepted")
			}
		})
	}
	valid.AccountNumber = ""
	if _, err := normalizeFinancialAccount(valid); err != nil {
		t.Fatal("bank account number is optional", err)
	}
	limit, day := 200000.50, int32(31)
	card := FinancialAccountInput{DisplayName: "Rewards", InstitutionName: "HDFC", AccountType: "credit_card", HolderName: "A Person", CardType: "Millennia", CardNetwork: " VISA ", LastFour: "0123", CreditLimit: &limit, StatementDay: &day}
	got, err = normalizeFinancialAccount(card)
	if err != nil || got.CardNetwork != "visa" || *got.CreditLimit != limit || *got.StatementDay != 31 {
		t.Fatalf("valid card: %+v, %v", got, err)
	}
	for name, change := range map[string]func(*FinancialAccountInput){
		"full card number":     func(v *FinancialAccountInput) { v.LastFour = "1234567890123456" },
		"unicode digits":       func(v *FinancialAccountInput) { v.LastFour = "１２３４" },
		"missing last four":    func(v *FinancialAccountInput) { v.LastFour = "" },
		"missing card type":    func(v *FinancialAccountInput) { v.CardType = "" },
		"invalid network":      func(v *FinancialAccountInput) { v.CardNetwork = "other" },
		"missing limit":        func(v *FinancialAccountInput) { v.CreditLimit = nil },
		"negative limit":       func(v *FinancialAccountInput) { n := -1.0; v.CreditLimit = &n },
		"infinite limit":       func(v *FinancialAccountInput) { n := math.Inf(1); v.CreditLimit = &n },
		"nan limit":            func(v *FinancialAccountInput) { n := math.NaN(); v.CreditLimit = &n },
		"missing day":          func(v *FinancialAccountInput) { v.StatementDay = nil },
		"day zero":             func(v *FinancialAccountInput) { n := int32(0); v.StatementDay = &n },
		"day 32":               func(v *FinancialAccountInput) { n := int32(32); v.StatementDay = &n },
		"bank subtype on card": func(v *FinancialAccountInput) { v.BankAccountSubtype = "savings" },
		"bank number on card":  func(v *FinancialAccountInput) { v.AccountNumber = "1234" },
	} {
		t.Run(name, func(t *testing.T) {
			in := card
			change(&in)
			if _, err := normalizeFinancialAccount(in); err == nil {
				t.Fatal("invalid card accepted")
			}
		})
	}
	for _, network := range []string{"visa", "mastercard", "american_express", "diners_club", "rupay"} {
		t.Run(network, func(t *testing.T) {
			in := card
			in.CardNetwork = network
			if _, err := normalizeFinancialAccount(in); err != nil {
				t.Fatal(err)
			}
		})
	}
}
