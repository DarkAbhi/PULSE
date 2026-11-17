package statementimport

import (
	"regexp"
	"strings"
	"unicode"
)

type categorySuggestion struct {
	primary     string
	child       string
	description string
	merchant    string
}

type narrationRule struct {
	phrases string
	categorySuggestion
}

var cardTaxCharge = regexp.MustCompile(`(?i)^\s*(?:IGST|CGST|SGST)[\s/-]+VPS(?:[0-9]|\b)`)
var fuelOutlet = regexp.MustCompile(
	`(?i)\b(?:fuel[\s/-]*station|petrol[\s/-]*pump|filling[\s/-]*station)[\p{L}]*\b`,
)

// Payment-purpose phrases precede merchant aliases because a narration can contain both.
var debitNarrationRules = []narrationRule{
	{phrases: "credit card bill|credit card payment|credit card repayment|card bill payment|cc bill payment|creditcard payment",
		categorySuggestion: categorySuggestion{primary: "Credit Bill", child: "credit card", description: "Credit card payment"}},
	{phrases: "atm cash withdrawal|atm withdrawal|atm wdl|cash withdrawal",
		categorySuggestion: categorySuggestion{primary: "Cash Withdrawal", description: "Cash withdrawal"}},
	{phrases: "self transfer|own account transfer|transfer to own account",
		categorySuggestion: categorySuggestion{primary: "Self-transfer", description: "Transfer between own accounts"}},
	{phrases: "upi lite top up|upi lite topup|upi lite load",
		categorySuggestion: categorySuggestion{primary: "Top-up", child: "UPI Lite", description: "UPI Lite top-up"}},
	{phrases: "wallet top up|wallet topup|wallet load|wallet recharge|add money to wallet",
		categorySuggestion: categorySuggestion{primary: "Top-up", child: "wallet", description: "Wallet top-up"}},
	{phrases: "emi payment|emi debit|emi repayment|emi prin|emi principal|emi int|loan installment|loan instalment",
		categorySuggestion: categorySuggestion{primary: "EMI", description: "Installment payment"}},
	{phrases: "retail finance charges|finance charge|finance charges|interest charge|interest charges",
		categorySuggestion: categorySuggestion{primary: "Hidden Charges", description: "Interest charge"}},
	{phrases: "igst vps|cgst vps|sgst vps",
		categorySuggestion: categorySuggestion{primary: "Tax", child: "GST", description: "GST on card charges"}},
	{phrases: "minimum balance charge|minimum balance charges|non maintenance charge|non maintenance charges",
		categorySuggestion: categorySuggestion{primary: "Hidden Charges", description: "Minimum balance fee"}},
	{phrases: "bank service fee|bank service charge|card annual fee|card renewal fee|annual card fee",
		categorySuggestion: categorySuggestion{primary: "Hidden Charges", description: "Bank fee"}},
	{phrases: "fuel surcharge",
		categorySuggestion: categorySuggestion{primary: "Hidden Charges", description: "Fuel surcharge"}},
	{phrases: "transfer to savings|savings contribution",
		categorySuggestion: categorySuggestion{primary: "Savings", description: "Savings contribution"}},
	{phrases: "business rent|office rent",
		categorySuggestion: categorySuggestion{primary: "Business", child: "rent", description: "Business rent"}},
	{phrases: "business inventory|inventory purchase",
		categorySuggestion: categorySuggestion{primary: "Business", child: "inventory", description: "Business inventory"}},
	{phrases: "school fee|school fees",
		categorySuggestion: categorySuggestion{primary: "Children", child: "school fee", description: "School fees"}},
	{phrases: "tuition fee|tuition fees",
		categorySuggestion: categorySuggestion{primary: "Children", child: "tuition fee", description: "Tuition fees"}},
	{phrases: "family support|parents allowance",
		categorySuggestion: categorySuggestion{primary: "Support", description: "Family support"}},
	{phrases: "pocket money",
		categorySuggestion: categorySuggestion{primary: "Support", child: "pocket money", description: "Pocket money"}},
	{phrases: "charity donation|charitable donation|donation payment",
		categorySuggestion: categorySuggestion{primary: "Donation", description: "Charity donation"}},
	{phrases: "gift card purchase|gift card load",
		categorySuggestion: categorySuggestion{primary: "Miscellaneous", child: "gift card", description: "Gift card purchase"}},
	{phrases: "birthday gift|wedding gift",
		categorySuggestion: categorySuggestion{primary: "Gift", description: "Gift payment"}},
	{phrases: "insurance premium|insurance renewal|lic premium",
		categorySuggestion: categorySuggestion{primary: "Insurance", description: "Insurance premium"}},
	{phrases: "income tax payment|advance tax payment|self assessment tax",
		categorySuggestion: categorySuggestion{primary: "Tax", child: "income tax", description: "Income tax payment"}},
	{phrases: "property tax payment",
		categorySuggestion: categorySuggestion{primary: "Tax", child: "property tax", description: "Property tax payment"}},
	{phrases: "gst payment",
		categorySuggestion: categorySuggestion{primary: "Tax", child: "GST", description: "GST payment"}},
	{phrases: "subscription renewal|subscription fee|subscription payment",
		categorySuggestion: categorySuggestion{primary: "Subscription", description: "Subscription payment"}},
	{phrases: "mutual fund sip|mutual fund purchase|mutual fund investment",
		categorySuggestion: categorySuggestion{primary: "Investment", child: "mutual funds", description: "Mutual fund investment"}},
	{phrases: "fixed deposit opening|fixed deposit booking|fixed deposit investment",
		categorySuggestion: categorySuggestion{primary: "Investment", child: "fixed deposit", description: "Fixed deposit"}},
	{phrases: "electricity bill|electricity payment|power bill payment",
		categorySuggestion: categorySuggestion{primary: "Bill", child: "electricity", description: "Electricity bill payment"}},
	{phrases: "rent payment|house rent|apartment rent",
		categorySuggestion: categorySuggestion{primary: "Bill", child: "rent", description: "Rent payment"}},
	{phrases: "water bill|water utility payment",
		categorySuggestion: categorySuggestion{primary: "Bill", child: "water", description: "Water bill payment"}},
	{phrases: "broadband bill|internet bill|broadband payment",
		categorySuggestion: categorySuggestion{primary: "Bill", child: "internet", description: "Internet bill payment"}},
	{phrases: "mobile bill|mobile recharge|phone bill|postpaid bill",
		categorySuggestion: categorySuggestion{primary: "Bill", child: "phone", description: "Phone bill payment"}},
	{phrases: "home renovation|home painting|pest control",
		categorySuggestion: categorySuggestion{primary: "Home"}},
	{phrases: "wedding venue|wedding catering|event venue booking",
		categorySuggestion: categorySuggestion{primary: "Events"}},
	{phrases: "cinema ticket|movie ticket|pvr inox|pvr cinemas|inox cinemas",
		categorySuggestion: categorySuggestion{primary: "Entertainment", child: "movies"}},
	{phrases: "hotel booking|hotel reservation",
		categorySuggestion: categorySuggestion{primary: "Travel", child: "hotel", description: "Hotel booking"}},
	{phrases: "visa fee|visa fees",
		categorySuggestion: categorySuggestion{primary: "Travel", child: "visa fees", description: "Visa fees"}},
	{phrases: "airbnb",
		categorySuggestion: categorySuggestion{primary: "Travel", child: "Airbnb", description: "Airbnb", merchant: "airbnb"}},
	{phrases: "pet food|pet grooming|veterinary clinic|vet consultation",
		categorySuggestion: categorySuggestion{primary: "Pet Care"}},
	{phrases: "apollo pharmacy|medplus|tata 1mg|pharmacy purchase",
		categorySuggestion: categorySuggestion{primary: "Medical", child: "medicines"}},
	{phrases: "hospital bill|medical consultation|diagnostic lab",
		categorySuggestion: categorySuggestion{primary: "Medical"}},
	{phrases: "salon appointment|haircut|barbershop",
		categorySuggestion: categorySuggestion{primary: "Personal", child: "grooming"}},
	{phrases: "gym membership|gym fee|gym fees",
		categorySuggestion: categorySuggestion{primary: "Fitness", child: "gym", description: "Gym membership"}},
	{phrases: "laundry service|laundry payment",
		categorySuggestion: categorySuggestion{primary: "Services", child: "laundry"}},
	{phrases: "courier service|courier charges",
		categorySuggestion: categorySuggestion{primary: "Services", child: "courier"}},
	{phrases: "forex purchase|currency exchange",
		categorySuggestion: categorySuggestion{primary: "Miscellaneous", child: "forex", description: "Currency exchange"}},
	{phrases: "zepto cafe|blinkit bistro",
		categorySuggestion: categorySuggestion{primary: "Food and Drinks"}},
	{phrases: "swiggy instamart|instamart",
		categorySuggestion: categorySuggestion{primary: "Groceries", description: "Swiggy Instamart", merchant: "instamart"}},
	{phrases: "blinkit",
		categorySuggestion: categorySuggestion{primary: "Groceries", description: "Blinkit", merchant: "blinkit"}},
	{phrases: "bigbasket|big basket",
		categorySuggestion: categorySuggestion{primary: "Groceries", description: "BigBasket", merchant: "bigbasket"}},
	{phrases: "zepto|grocery purchase",
		categorySuggestion: categorySuggestion{primary: "Groceries"}},
	{phrases: "swiggy",
		categorySuggestion: categorySuggestion{primary: "Food and Drinks", child: "Swiggy", description: "Swiggy", merchant: "swiggy"}},
	{phrases: "zomato",
		categorySuggestion: categorySuggestion{primary: "Food and Drinks", child: "Zomato", description: "Zomato", merchant: "zomato"}},
	{phrases: "uber eats",
		categorySuggestion: categorySuggestion{primary: "Food and Drinks", description: "Uber Eats", merchant: "uber eats"}},
	{phrases: "restaurant bill|food delivery",
		categorySuggestion: categorySuggestion{primary: "Food and Drinks"}},
	{phrases: "uber",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "Uber", description: "Uber", merchant: "uber"}},
	{phrases: "ola cabs|ola ride",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "cab", description: "Ola ride", merchant: "ola cabs"}},
	{phrases: "rapido|rappido",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "Rappido", description: "Rapido", merchant: "rapido"}},
	{phrases: "irctc|railway ticket",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "train", description: "Train ticket"}},
	{phrases: "fastag|fast tag",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "Fastag", description: "FASTag payment"}},
	{phrases: "fuel purchase|petrol pump|fuel station|filling station",
		categorySuggestion: categorySuggestion{primary: "Transport", child: "fuel"}},
	{phrases: "zerodha|zerod ha",
		categorySuggestion: categorySuggestion{primary: "Investment", description: "Zerodha payment", merchant: "zerodha"}},
	{phrases: "myntra|ajio|flipkart purchase|amazon retail|amazon marketplace|amazon seller services",
		categorySuggestion: categorySuggestion{primary: "Shopping"}},
	{phrases: "netflix",
		categorySuggestion: categorySuggestion{primary: "Subscription", child: "Netflix", description: "Netflix", merchant: "netflix"}},
	{phrases: "spotify premium",
		categorySuggestion: categorySuggestion{primary: "Subscription", child: "Spotify", description: "Spotify Premium"}},
	{phrases: "amazon pay",
		categorySuggestion: categorySuggestion{description: "Amazon Pay"}},
	{phrases: "cred club|cred",
		categorySuggestion: categorySuggestion{description: "CRED payment"}},
}

func classifyNarration(text string, direction string) categorySuggestion {
	words := strings.FieldsFunc(strings.ToLower(text), func(r rune) bool {
		return !unicode.IsLetter(r) && !unicode.IsNumber(r)
	})
	narration := " " + strings.Join(words, " ") + " "
	contains := func(phrase string) bool { return strings.Contains(narration, " "+phrase+" ") }
	refund := contains("refund") || contains("purchase return")
	reversal := contains("reversal")
	switch direction {
	case "credit":
		switch {
		case contains("income tax refund"):
			return categorySuggestion{primary: "Tax", child: "income tax", description: "Income tax refund"}
		case refund:
			return categorySuggestion{primary: "Return", description: "Refund"}
		case reversal:
			return categorySuggestion{primary: "Return", description: "Payment reversal"}
		case contains("self transfer") || contains("own account transfer"):
			return categorySuggestion{primary: "Self-transfer", description: "Transfer between own accounts"}
		case contains("monthly interest credit"):
			return categorySuggestion{description: "Monthly bank interest"}
		case contains("savings interest credit") || contains("bank interest credit"):
			return categorySuggestion{description: "Bank interest"}
		case contains("interest credit"):
			return categorySuggestion{description: "Interest credit"}
		case contains("bbps payment received") || contains("payment received thank you"):
			return categorySuggestion{description: "Credit card payment received"}
		case contains("cashback"):
			return categorySuggestion{description: "Cashback"}
		case contains("salary"):
			return categorySuggestion{description: "Salary credit"}
		}
	case "debit":
		if refund || reversal {
			return categorySuggestion{}
		}
		if cardTaxCharge.MatchString(text) {
			return categorySuggestion{primary: "Tax", child: "GST", description: "GST on card charges"}
		}
		if strings.HasPrefix(narration, " emi ") {
			return categorySuggestion{primary: "EMI", description: "Installment payment"}
		}
		for _, rule := range debitNarrationRules {
			for phrase := range strings.SplitSeq(rule.phrases, "|") {
				if contains(phrase) {
					return rule.categorySuggestion
				}
			}
		}
		// Card statements can join a fuel outlet's business type and location.
		if fuelOutlet.MatchString(text) {
			return categorySuggestion{primary: "Transport", child: "fuel"}
		}
	}
	return categorySuggestion{}
}
