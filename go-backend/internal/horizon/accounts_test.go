package horizon

import (
	"strings"
	"testing"
)

func TestNormalizeFinancialAccount(t *testing.T) {
	valid := FinancialAccountInput{DisplayName: " Savings ", InstitutionName: " Example Bank ", AccountType: "bank_account", BankAccountSubtype: "savings", Currency: " inr ", CountryCode: " in ", LastFour: " 1234 "}
	got, err := normalizeFinancialAccount(valid, "INR")
	if err != nil || got.DisplayName != "Savings" || got.InstitutionName != "Example Bank" || got.Currency != "INR" || got.CountryCode != "IN" || got.LastFour != "1234" {
		t.Fatal("valid account was not normalized")
	}
	for name, change := range map[string]func(*FinancialAccountInput){
		"empty name":           func(v *FinancialAccountInput) { v.DisplayName = " " },
		"long name":            func(v *FinancialAccountInput) { v.DisplayName = strings.Repeat("x", 101) },
		"control character":    func(v *FinancialAccountInput) { v.InstitutionName = "Bank\nName" },
		"invalid type":         func(v *FinancialAccountInput) { v.AccountType = "debit_card" },
		"invalid subtype":      func(v *FinancialAccountInput) { v.BankAccountSubtype = "unknown" },
		"card subtype":         func(v *FinancialAccountInput) { v.AccountType = "credit_card" },
		"different currency":   func(v *FinancialAccountInput) { v.Currency = "USD" },
		"unsupported currency": func(v *FinancialAccountInput) { v.Currency = "ZZZ" },
		"country digits":       func(v *FinancialAccountInput) { v.CountryCode = "12" },
		"full identifier":      func(v *FinancialAccountInput) { v.LastFour = "1234567890123456" },
		"identifier letters":   func(v *FinancialAccountInput) { v.LastFour = "abcd" },
		"unicode digits":       func(v *FinancialAccountInput) { v.LastFour = "１２３４" },
	} {
		t.Run(name, func(t *testing.T) {
			in := valid
			change(&in)
			if _, err := normalizeFinancialAccount(in, "INR"); err == nil {
				t.Fatal("invalid account accepted")
			}
		})
	}
	valid.AccountType = "credit_card"
	valid.BankAccountSubtype = ""
	valid.LastFour = ""
	valid.CountryCode = ""
	if _, err := normalizeFinancialAccount(valid, "INR"); err != nil {
		t.Fatal("optional card details should not be required")
	}
}
