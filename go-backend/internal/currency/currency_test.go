package currency

import "testing"

func TestValid(t *testing.T) {
	for _, code := range []string{"GBP", "USD", "EUR", "INR", "JPY", "KWD"} {
		t.Run(code, func(t *testing.T) {
			if !Valid(code) {
				t.Fatalf("rejected %s", code)
			}
		})
	}
	for _, code := range []string{"", "gbp", " GBP", "ZZZ", "XXX", "£", "USD EUR"} {
		t.Run("invalid_"+code, func(t *testing.T) {
			if Valid(code) {
				t.Fatalf("accepted %q", code)
			}
		})
	}
}
