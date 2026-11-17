package statementimport

import (
	"regexp"
	"strings"
	"unicode"

	horizonquery "github.com/DarkAbhi/life-backend/internal/horizon/query"
)

type categoryPath struct {
	parentID int64
	name     string
}

func defaultCategoryPaths(categories []horizonquery.ListCategoriesRow) map[categoryPath]int64 {
	paths := make(map[categoryPath]int64, len(categories))
	for _, category := range categories {
		if category.IsDefault && !category.UserID.Valid {
			paths[categoryPath{parentID: category.ParentID.Int64, name: strings.ToLower(category.Name)}] = category.ID
		}
	}
	return paths
}

func suggestedCategoryID(paths map[categoryPath]int64, suggestion categorySuggestion) *int64 {
	parent, ok := paths[categoryPath{name: strings.ToLower(suggestion.primary)}]
	if !ok {
		return nil
	}
	for _, name := range []string{strings.ToLower(suggestion.child), "others"} {
		if id, ok := paths[categoryPath{parentID: parent, name: name}]; ok {
			return &id
		}
	}
	return &parent
}

var (
	upiAddress      = regexp.MustCompile(`(?i)^([a-z0-9][a-z0-9._-]*)@[a-z][a-z0-9.-]*$`)
	upiOpaque       = regexp.MustCompile(`(?i)^(?:x+[0-9]+|[0-9]{6,}|[a-z0-9]{12,})$`)
	referenceSuffix = regexp.MustCompile(
		`(?i)\s+[(]?(?:REF(?:ERENCE)?(?:\s+NO)?|RRN|UTR|TXN\s+ID)\s*[:#-]?\s*[a-z0-9]*[0-9][a-z0-9-]*[)]?$`,
	)
	paymentPrefix   = regexp.MustCompile(`(?i)^(?:INB[ /-]+)?(IMPS|NEFT|RTGS|POS|ECOM|NACH|ACH)[ /:-]+`)
	bankReference   = regexp.MustCompile(`(?i)^(?:[a-z]{4}0[a-z0-9]{6}|[a-z]{0,8}[0-9]{6,}|[0-9]{6,}[a-z0-9]*)$`)
	bankRoutingCode = regexp.MustCompile(`(?i)^(?:HDFC|SBIN|ICIC|UTIB|KKBK|IDFB|FDRL|YESB|PUNB|CNRB|BKID|BARB|IOBA|INDB|UBIN)$`)
)

type narrationSuggestion struct {
	categorySuggestion
	name string
	key  string
}

func suggestNarration(row LedgerRow) narrationSuggestion {
	clean := strings.Join(strings.FieldsFunc(row.Description, func(r rune) bool {
		return unicode.IsSpace(r) || unicode.IsControl(r)
	}), " ")
	clean = strings.TrimSpace(referenceSuffix.ReplaceAllString(clean, ""))

	payee, handle, purpose, upi := upiPayee(clean)
	evidence, identity, name := clean, clean, clean
	rail := "UPI"
	structured := upi
	if !upi {
		payee, rail, structured = paymentPayee(clean)
	}
	if structured {
		if upi {
			merchantHandle, _, _ := strings.Cut(handle, "@")
			evidence = strings.Join([]string{payee, merchantHandle, purpose}, " ")
		}
		identity = payee
		if handle != "" {
			identity = handle
		}
		name = rail + " payment"
		if row.Type == "credit" {
			name = rail + " received"
		}
		if payee != "" {
			link := " to "
			if row.Type == "credit" {
				link = " from "
			}
			name += link + payee
		}
	}
	category := classifyNarration(evidence, row.Type)
	if category.merchant != "" {
		identity = category.merchant
	}
	if category.description != "" {
		name = category.description
	}
	if name == "" {
		name = "Statement transaction"
	}
	runes := []rune(name)
	suggestion := narrationSuggestion{
		categorySuggestion: category,
		name:               strings.TrimSpace(string(runes[:min(255, len(runes))])),
	}
	if identity != "" {
		// Scope a correction to the same direction and purchase or payment context.
		suggestion.key = digest([]byte(strings.Join([]string{
			"merchant-v2", row.Type, category.primary, category.child, strings.ToLower(identity),
		}, "\x00")))
	}
	return suggestion
}

func upiPayee(text string) (payee, handle, purpose string, ok bool) {
	upper := strings.ToUpper(text)
	if upper == "UPI" {
		return "", "", "", true
	}
	separator := "/"
	if strings.HasPrefix(upper, "UPI-") {
		separator = "-"
	} else if !strings.HasPrefix(upper, "UPI/") {
		return "", "", "", false
	}
	parts := strings.Split(text, separator)
	payeeIndex := -1
	for index := 1; index < len(parts); index++ {
		part := strings.TrimSpace(parts[index])
		switch strings.ToUpper(part) {
		case "", "DR", "CR", "DEBIT", "CREDIT", "P2P", "P2M", "P2A", "PAYMENT", "TRANSFER":
			continue
		case "REFUND", "REVERSAL", "CASHBACK":
			purpose = part
			continue
		}
		if address := upiAddress.FindStringSubmatch(part); address != nil {
			if handle == "" {
				handle = part
			}
			continue
		}
		if bankRoutingCode.MatchString(part) {
			continue
		}
		if payeeIndex == index-2 && payeeIndex >= 0 && index >= 2 &&
			bankRoutingCode.MatchString(strings.TrimSpace(parts[index-1])) {
			// Some banks put the handle without its provider after the bank code.
			if readablePayee(part) && !strings.ContainsAny(part, " @") {
				handle = part
			}
		} else if payeeIndex < 0 && handle == "" && readablePayee(part) {
			payee, payeeIndex = part, index
		}
	}
	return payee, handle, purpose, true
}

func readablePayee(text string) bool {
	if !strings.ContainsFunc(text, unicode.IsLetter) || strings.Contains(text, "@") {
		return false
	}
	return !upiOpaque.MatchString(text) || !strings.ContainsFunc(text, unicode.IsDigit)
}

func paymentPayee(text string) (payee, rail string, ok bool) {
	match := paymentPrefix.FindStringSubmatch(text)
	if match == nil {
		return "", "", false
	}
	rail = strings.ToUpper(match[1])
	remainder := text[len(match[0]):]
	for _, part := range strings.FieldsFunc(remainder, func(r rune) bool {
		return r == '/' || r == ':' || r == '-'
	}) {
		part = strings.TrimSpace(part)
		switch strings.ToUpper(part) {
		case "", "DR", "CR", "DEBIT", "CREDIT", "P2A", "P2P", "P2M", "INB", "MB", "TO", "FROM", "PUR", "PURCHASE":
			continue
		}
		if bankReference.MatchString(part) || bankRoutingCode.MatchString(part) || !readablePayee(part) {
			continue
		}
		return part, rail, true
	}
	return "", rail, true
}
