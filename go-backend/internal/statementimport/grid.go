package statementimport

import (
	"regexp"
	"strconv"
	"strings"
	"time"
)

// A ruled table can omit merged cells near its left edge. Use its horizontal
// row bounds and the full header geometry to recover those columns.
func assembleGrid(pages []Page, p profile) (Ledger, *failure) {
	ledger := Ledger{Profile: p}
	accountRe := regexp.MustCompile(p.AccountPattern)
	var headerOpening, debitCount, creditCount, debitTotal, creditTotal int64
	var printedOpening, summaryFound bool
	var parsedDebits, parsedCredits, parsedDebitTotal, parsedCreditTotal int64
	for _, page := range pages {
		for _, line := range lines(page, p.LineTolerance) {
			if match := accountRe.FindStringSubmatch(line.text); len(match) == 2 {
				if ledger.LastFour != "" && ledger.LastFour != match[1] {
					return ledger, accountMismatch
				}
				ledger.LastFour = match[1]
			}
		}
		for _, table := range page.Tables {
			if len(table.RowBounds) < 2 {
				continue
			}
			head := region(page, table.RowBounds[0], p.LineTolerance)
			heading := strings.ToLower(head.text)
			if strings.HasPrefix(heading, p.SummaryPrefix) && strings.Contains(heading, p.SummaryHeader) {
				if summaryFound || len(table.RowBounds) != 2 {
					return ledger, balancesUnavailable
				}
				values := strings.Fields(region(page, table.RowBounds[1], p.LineTolerance).text)
				if len(values) != 8 {
					return ledger, balancesUnavailable
				}
				var valid bool
				ledger.Opening, valid = p.markedBalance(values[0] + " " + values[1])
				if !valid {
					return ledger, balancesUnavailable
				}
				ledger.Closing, valid = p.markedBalance(values[6] + " " + values[7])
				if !valid {
					return ledger, balancesUnavailable
				}
				debitCount, valid = gridCount(values[2])
				if !valid {
					return ledger, balancesUnavailable
				}
				creditCount, valid = gridCount(values[3])
				if !valid {
					return ledger, balancesUnavailable
				}
				debitTotal, valid = p.money(values[4])
				if !valid {
					return ledger, balancesUnavailable
				}
				creditTotal, valid = p.money(values[5])
				if !valid {
					return ledger, balancesUnavailable
				}
				summaryFound = true
				continue
			}
			if !strings.HasPrefix(heading, p.RegionPrefix) {
				continue
			}
			cuts := p.boundaries(head)
			if cuts == nil {
				return ledger, invalidRow
			}
			for _, bbox := range table.RowBounds[1:] {
				cells := columns(region(page, bbox, p.LineTolerance), cuts)
				if strings.EqualFold(cells[2], p.OpeningLabel) {
					var valid bool
					headerOpening, valid = p.markedBalance(cells[6])
					if !valid || printedOpening || len(ledger.Rows) > 0 {
						return ledger, balancesUnavailable
					}
					printedOpening = true
					continue
				}
				dateParts := strings.Fields(cells[0])
				if len(dateParts) < 3 || cells[2] == "" || len(ledger.Rows) >= 10_000 {
					return ledger, invalidRow
				}
				date, err := time.Parse(p.DateFormat, strings.Join(dateParts[:3], " "))
				balance, valid := p.markedBalance(cells[6])
				if err != nil || !valid {
					return ledger, invalidRow
				}
				direction, value := "debit", cells[4]
				if emptyAmount(value) {
					direction, value = "credit", cells[5]
				} else if !emptyAmount(cells[5]) {
					return ledger, invalidRow
				}
				amount, valid := p.money(value)
				if !valid || amount <= 0 || amount > 999_999_999_999 {
					return ledger, invalidRow
				}
				ledger.Rows = append(ledger.Rows, LedgerRow{
					Date: date.Format(time.DateOnly), Description: cells[2], Amount: amount,
					Type: direction, Balance: balance, Reference: cells[3],
				})
				if direction == "debit" {
					parsedDebits++
					parsedDebitTotal += amount
				} else {
					parsedCredits++
					parsedCreditTotal += amount
				}
			}
		}
	}
	if !summaryFound || !printedOpening || len(ledger.Rows) == 0 {
		return ledger, balancesUnavailable
	}
	if ledger.Opening != headerOpening || debitCount != parsedDebits || creditCount != parsedCredits ||
		debitTotal != parsedDebitTotal || creditTotal != parsedCreditTotal || !reconciles(ledger.Opening, ledger.Closing, ledger.Rows) {
		return ledger, reconciliationFailed
	}
	return ledger, nil
}

func region(page Page, bbox [4]float64, tolerance float64) line {
	var cropped Page
	for _, word := range page.Words {
		if word.Top >= bbox[1] && word.Top < bbox[3] && word.X0 < bbox[2] {
			cropped.Words = append(cropped.Words, word)
		}
	}
	var result line
	text := make([]string, 0)
	for _, row := range lines(cropped, tolerance) {
		result.words = append(result.words, row.words...)
		text = append(text, row.text)
	}
	result.text = strings.Join(text, " ")
	return result
}

func gridCount(raw string) (int64, bool) {
	n, err := strconv.ParseInt(raw, 10, 64)
	return n, err == nil && n >= 0 && n <= 10_000
}

func emptyAmount(raw string) bool {
	return raw == "" || raw == "-"
}

func (p profile) markedBalance(raw string) (int64, bool) {
	parts := strings.Fields(raw)
	if len(parts) != 2 {
		return 0, false
	}
	amount, valid := p.money(parts[0])
	if !valid || amount < 0 {
		return 0, false
	}
	switch strings.ToUpper(parts[1]) {
	case "CR":
		return amount, true
	case "DR":
		return -amount, true
	default:
		return 0, false
	}
}
