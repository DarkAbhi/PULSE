package statementimport

import "testing"

func TestClassifyNarration(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name      string
		narration string
		direction string
		want      categorySuggestion
	}{
		{
			name: "merchant name across UPI separators", narration: "UPI/DR/1234/SWIGGY/HDFC", direction: "debit",
			want: categorySuggestion{primary: "Food and Drinks", child: "Swiggy", description: "Swiggy", merchant: "swiggy"},
		},
		{
			name: "explicit Instamart is groceries", narration: "SWIGGY / InstaMart", direction: "debit",
			want: categorySuggestion{primary: "Groceries", description: "Swiggy Instamart", merchant: "instamart"},
		},
		{
			name: "food delivery before ride merchant", narration: "UBER * EATS", direction: "debit",
			want: categorySuggestion{primary: "Food and Drinks", description: "Uber Eats", merchant: "uber eats"},
		},
		{
			name: "card bill before card brand", narration: "HDFC SWIGGY CARD BILL PAYMENT", direction: "debit",
			want: categorySuggestion{primary: "Credit Bill", child: "credit card", description: "Credit card payment"},
		},
		{
			name: "card fee before card brand", narration: "HDFC SWIGGY CREDIT CARD ANNUAL FEE", direction: "debit",
			want: categorySuggestion{primary: "Hidden Charges", description: "Bank fee"},
		},
		{
			name: "broker purchase without product", narration: "ICCL ZER / zerodha", direction: "debit",
			want: categorySuggestion{primary: "Investment", description: "Zerodha payment", merchant: "zerodha"},
		},
		{
			name: "NEFT vendor tokens", narration: "NEFT-HDFC0000000-ZERODHA-REF000123", direction: "debit",
			want: categorySuggestion{primary: "Investment", description: "Zerodha payment", merchant: "zerodha"},
		},
		{
			name: "IMPS vendor tokens", narration: "IMPS/P2M/000123/SWIGGY/REF000456", direction: "debit",
			want: categorySuggestion{primary: "Food and Drinks", child: "Swiggy", description: "Swiggy", merchant: "swiggy"},
		},
		{
			name: "card EMI principal", narration: "MER EMI, PRIN: 000000 (REF000123)", direction: "debit",
			want: categorySuggestion{primary: "EMI", description: "Installment payment"},
		},
		{
			name: "standalone EMI charge", narration: "EMI 000000 REF000123", direction: "debit",
			want: categorySuggestion{primary: "EMI", description: "Installment payment"},
		},
		{
			name: "card tax routing", narration: "IGST-VPS000000-REF000123", direction: "debit",
			want: categorySuggestion{primary: "Tax", child: "GST", description: "GST on card charges"},
		},
		{
			name: "card tax tokenized", narration: "IGST-VPS-REF000123", direction: "debit",
			want: categorySuggestion{primary: "Tax", child: "GST", description: "GST on card charges"},
		},
		{
			name: "refund before merchant", narration: "refund: SWIGGY", direction: "credit",
			want: categorySuggestion{primary: "Return", description: "Refund"},
		},
		{
			name: "standalone category", narration: "ATM/WDl/954", direction: "debit",
			want: categorySuggestion{primary: "Cash Withdrawal", description: "Cash withdrawal"},
		},
		{
			name: "specific utility", narration: "ELECTRICITY-BILL PAYMENT", direction: "debit",
			want: categorySuggestion{primary: "Bill", child: "electricity", description: "Electricity bill payment"},
		},
		{
			name: "fuel outlet with joined city", narration: "KRJ FUEL STATIONMYSORE", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "fuel outlet extracted from HDFC", narration: "K R J FUEL STATIONMYSORE", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "fuel outlet with separate city", narration: "KRJ FUEL STATION MYSORE", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "compact fuel outlet", narration: "KRJ FUELSTATIONMYSORE", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "petrol pump with joined city", narration: "EXAMPLE PETROL PUMPBENGALURU", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "filling station with joined city", narration: "EXAMPLE FILLING STATIONPUNE", direction: "debit",
			want: categorySuggestion{primary: "Transport", child: "fuel"},
		},
		{
			name: "fuel surcharge is a fee", narration: "FUEL SURCHARGE KRJ FUEL STATIONMYSORE", direction: "debit",
			want: categorySuggestion{primary: "Hidden Charges", description: "Fuel surcharge"},
		},
		{
			name: "fuel refund", narration: "REFUND KRJ FUEL STATIONMYSORE", direction: "credit",
			want: categorySuggestion{primary: "Return", description: "Refund"},
		},
		{name: "fuel outlet credit without purchase evidence", narration: "KRJ FUEL STATIONMYSORE", direction: "credit"},
		{name: "fuel substring in another word", narration: "REFUEL STATIONMYSORE", direction: "debit"},
		{
			name: "interest is a description only", narration: "MONTHLY INTEREST CREDIT", direction: "credit",
			want: categorySuggestion{description: "Monthly bank interest"},
		},
		{
			name: "bank interest is readable", narration: "INTEREST CREDIT", direction: "credit",
			want: categorySuggestion{description: "Interest credit"},
		},
		{
			name: "card bill receipt", narration: "BBPS PAYMENT RECEIVED", direction: "credit",
			want: categorySuggestion{description: "Credit card payment received"},
		},
		{
			name: "card cashback", narration: "5% SWIGGY CASHBACK", direction: "credit",
			want: categorySuggestion{description: "Cashback"},
		},
		{
			name: "ambiguous payment provider still has a readable name", narration: "AMAZON PAY BANGALORE REF000123", direction: "debit",
			want: categorySuggestion{description: "Amazon Pay"},
		},
		{
			name: "ambiguous CRED payment keeps its purpose unknown", narration: "UPI/DR/000123/CRED CLUB/cred.club@ybl", direction: "debit",
			want: categorySuggestion{description: "CRED payment"},
		},
		{
			name: "explicit wallet load overrides provider", narration: "AMAZON PAY WALLET TOP UP", direction: "debit",
			want: categorySuggestion{primary: "Top-up", child: "wallet", description: "Wallet top-up"},
		},
		{
			name: "salary narration provides a description", narration: "NEFT/REF000123/SALARY", direction: "credit",
			want: categorySuggestion{description: "Salary credit"},
		},
		{name: "merchant lookalike", narration: "Swiggy123", direction: "debit"},
		{name: "merchant prefix is opaque", narration: "TSWIGGY SF91XS", direction: "debit"},
		{name: "opaque spelling", narration: "SRIGI", direction: "debit"},
		{name: "merchant name without outgoing payment", narration: "Zomato", direction: "credit"},
		{name: "debit reversal is not a purchase", narration: "Zomato reversal", direction: "debit"},
	}
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := classifyNarration(tc.narration, tc.direction); got != tc.want {
				t.Fatalf("classifyNarration(%q, %q) = %+v, want %+v", tc.narration, tc.direction, got, tc.want)
			}
		})
	}
}
