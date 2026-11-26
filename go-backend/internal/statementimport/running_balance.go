package statementimport

import (
	"regexp"
	"strings"
	"time"
)

var balanceAtDate = regexp.MustCompile(`\b[0-9]{2}-[0-9]{2}-[0-9]{2,4}\s*:\s*(-?[0-9][0-9,]*\.[0-9]{2})$`)

func assembleRunningBalance(pages [][]line, p profile) (Ledger, *failure) {
	result := Ledger{Profile: p}
	account := regexp.MustCompile(p.AccountPattern)
	balanceLine := regexp.MustCompile(`(?i)\b(opening|closing)(?: null)? balance on [0-9-]+:\s*([0-9.,-]+)`)
	var openingFound, closingFound, totalsFound bool
	var debitTotal, creditTotal int64
	for _, page := range pages {
		var cuts []float64
		var lastTop float64
		var nextDescription string
		var afterHeader bool
		for _, row := range page {
			if match := account.FindStringSubmatch(row.text); len(match) == 2 {
				if result.LastFour != "" && result.LastFour != match[1] {
					return result, accountMismatch
				}
				result.LastFour = match[1]
			}
			if match := balanceLine.FindStringSubmatch(row.text); len(match) == 3 {
				balance, ok := p.money(match[2])
				if !ok {
					return result, balancesUnavailable
				}
				if strings.EqualFold(match[1], "opening") {
					if openingFound || len(result.Rows) != 0 {
						return result, balancesUnavailable
					}
					result.Opening, openingFound = balance, true
				} else {
					if closingFound || len(result.Rows) == 0 {
						return result, balancesUnavailable
					}
					result.Closing, closingFound = balance, true
					cuts = nil
				}
				continue
			}
			if header := p.boundaries(row); len(header) == 6 {
				cuts = header
				afterHeader = true
				continue
			}
			if cuts == nil {
				continue
			}
			c := columns(row, cuts)
			// SBI overlays placeholder text on the opening label. Its first
			// table row still prints the dated balance and four null cells.
			readOpening := afterHeader && p.OpeningPlaceholders && !openingFound && len(result.Rows) == 0 &&
				c[3] == "null" && c[4] == "null" && c[5] == "null" && c[6] == "null"
			afterHeader = false
			if readOpening {
				match := balanceAtDate.FindStringSubmatch(c[2])
				if len(match) != 2 {
					return result, balancesUnavailable
				}
				balance, ok := p.money(match[1])
				if !ok {
					return result, balancesUnavailable
				}
				result.Opening, openingFound = balance, true
				continue
			}
			text := strings.ToLower(row.text)
			if strings.HasPrefix(text, p.OpeningLabel) && p.WholeAmounts {
				balance, ok := p.bankBalance(c[6])
				if !ok || openingFound || len(result.Rows) != 0 {
					return result, balancesUnavailable
				}
				result.Opening, openingFound = balance, true
				continue
			}
			debitText, creditText := c[4], c[5]
			if !p.WholeAmounts {
				debitText, creditText = c[5], c[4]
			}
			if p.SummaryHeader != "" && strings.HasPrefix(text, p.SummaryHeader) {
				debit, validDebit := p.bankAmount(debitText)
				credit, validCredit := p.bankAmount(creditText)
				if totalsFound || !validDebit || !validCredit {
					return result, balancesUnavailable
				}
				totalsFound, debitTotal, creditTotal = true, debit, credit
				cuts = nil
				continue
			}
			dateText, _, _ := strings.Cut(c[1], " ")
			date, err := time.Parse(p.DateFormat, dateText)
			if err != nil {
				if dateText != "" {
					if _, validBalance := p.bankBalance(c[6]); validBalance {
						return result, invalidRow
					}
				}
				if c[1] == "" && c[2] != "" && c[3] == "" && c[4] == "" && c[5] == "" && c[6] == "" {
					if lastTop > 0 && row.top-lastTop <= p.ContinuationGap {
						tail := &result.Rows[len(result.Rows)-1]
						tail.Description += " " + c[2]
						lastTop = row.top
					} else {
						nextDescription = c[2]
					}
				}
				continue
			}
			debit, validDebit := p.bankAmount(debitText)
			credit, validCredit := p.bankAmount(creditText)
			balance, validBalance := p.bankBalance(c[6])
			description := strings.TrimSpace(nextDescription + " " + c[2])
			if !validDebit || !validCredit || !validBalance || (debit > 0) == (credit > 0) ||
				description == "" || len(result.Rows) >= 10_000 {
				return result, invalidRow
			}
			tx := LedgerRow{
				Date: date.Format(time.DateOnly), Description: description, Type: "debit", Amount: debit,
				Balance: balance, Reference: c[3],
			}
			if credit > 0 {
				tx.Amount, tx.Type = credit, "credit"
			}
			result.Rows = append(result.Rows, tx)
			nextDescription, lastTop = "", row.top
		}
	}
	if !openingFound || len(result.Rows) == 0 || (!closingFound && !totalsFound) {
		return result, balancesUnavailable
	}
	if totalsFound {
		debit, credit := directionTotals(result.Rows)
		if debit != debitTotal || credit != creditTotal {
			return result, reconciliationFailed
		}
		closing := result.Opening + credit - debit
		if closingFound && closing != result.Closing {
			return result, reconciliationFailed
		}
		result.Closing = closing
	}
	if !reconciles(result.Opening, result.Closing, result.Rows) {
		return result, reconciliationFailed
	}
	return result, nil
}

func (p profile) bankAmount(raw string) (int64, bool) {
	if emptyAmount(raw) || raw == "0" {
		return 0, true
	}
	if p.WholeAmounts && !strings.Contains(raw, p.DecimalSep) {
		raw += p.DecimalSep + "00"
	}
	value, ok := p.money(raw)
	return value, ok && value >= 0 && value <= 999_999_999_999
}

func (p profile) bankBalance(raw string) (int64, bool) {
	if !p.WholeAmounts {
		return p.money(raw)
	}
	amount, marker, ok := strings.Cut(raw, " ")
	if !ok {
		return 0, false
	}
	if !strings.Contains(amount, p.DecimalSep) {
		amount += p.DecimalSep + "00"
	}
	return p.markedBalance(amount + " " + marker)
}
