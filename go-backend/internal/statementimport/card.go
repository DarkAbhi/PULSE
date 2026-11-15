package statementimport

import (
	"regexp"
	"sort"
	"strings"
	"time"
)

func assembleCard(pages [][]line, p profile) (Ledger, *failure) {
	result := Ledger{Profile: p}
	account := regexp.MustCompile(p.AccountPattern)
	footer := regexp.MustCompile(p.FooterPattern)
	descriptionColumn, amountColumn := 3, len(p.Anchors)
	prefixedAmount := p.SignConvention == "credit_prefix"
	if p.DescriptionColumn != 0 {
		descriptionColumn = p.DescriptionColumn
	}
	var totals map[string]int64
	for _, page := range pages {
		var cuts []float64
		var lastTop float64
		for i, row := range page {
			text := strings.ToLower(row.text)
			if match := account.FindStringSubmatch(row.text); len(match) == 2 {
				if result.LastFour != "" && result.LastFour != match[1] {
					return result, accountMismatch
				}
				result.LastFour = match[1]
			}
			if len(p.SummaryLabels) == 0 && strings.Contains(text, p.SummaryHeader) && strings.Contains(text, p.OpeningLabel) {
				if totals != nil {
					return result, balancesUnavailable
				}
				totals = readCardTotals(page[i+1:], row.top, p)
				if totals == nil {
					return result, balancesUnavailable
				}
				result.Opening, result.Closing = totals["opening"], totals["closing"]
				continue
			}
			if strings.HasPrefix(text, p.RegionPrefix) {
				if header := p.boundaries(row); len(header) == amountColumn {
					cuts = header
					lastTop = 0
					continue
				}
			}
			if cuts == nil {
				continue
			}
			if footer.MatchString(text) {
				cuts = nil
				continue
			}
			cells := columns(row, cuts)
			dateText := cells[1]
			if prefixedAmount {
				dateText, _, _ = strings.Cut(dateText, " ")
			}
			date, dateErr := time.Parse(p.DateFormat, strings.TrimSuffix(dateText, "|"))
			if dateErr != nil {
				if dateText != "" && p.DescriptionColumn != 0 {
					if _, _, validAmount := p.cardAmount(cells[amountColumn]); validAmount {
						return result, invalidRow
					}
				}
				if cells[1] == "" && cells[descriptionColumn] != "" && cells[amountColumn] == "" &&
					(p.DescriptionColumn != 0 || (cells[2] == "" && cells[4] == "")) &&
					lastTop > 0 && row.top-lastTop <= p.ContinuationGap {
					tx := &result.Rows[len(result.Rows)-1]
					tx.Description += " " + cells[descriptionColumn]
					lastTop = row.top
				}
				continue
			}
			if cells[descriptionColumn] == "" || len(result.Rows) >= 10_000 {
				return result, invalidRow
			}
			amount, direction, ok := p.cardAmount(cells[amountColumn])
			if !ok {
				return result, invalidRow
			}
			reference := ""
			if p.DescriptionColumn == 0 {
				reference = cells[2]
			}
			result.Rows = append(result.Rows, LedgerRow{
				Date: date.Format(time.DateOnly), Description: cells[descriptionColumn], Amount: amount,
				Type: direction, StableReference: reference,
			})
			lastTop = row.top
		}
	}
	if len(p.SummaryLabels) > 0 {
		totals = readLabeledCardTotals(pages, p)
		if totals != nil {
			result.Opening, result.Closing = totals["opening"], totals["closing"]
		}
	}
	if totals == nil || len(result.Rows) == 0 {
		return result, balancesUnavailable
	}
	debit, credit := directionTotals(result.Rows)
	exactDue := result.Opening + debit - credit
	printedDue := exactDue
	if p.RoundedCardDue {
		// The payable amount is rounded on this layout, while its debit and credit totals retain paise.
		printedDue = roundCardDue(exactDue)
	}
	if debit != totals["debit"]+totals["fees"]+totals["cash"] || credit != totals["credit"]+totals["payment"] ||
		printedDue != result.Closing {
		return result, reconciliationFailed
	}
	// Keep the exact balance for revalidating edits against the source rows.
	result.Closing = exactDue
	return result, nil
}

func roundCardDue(minor int64) int64 {
	if minor < 0 {
		return -roundCardDue(-minor)
	}
	return (minor + 50) / 100 * 100
}

func readCardTotals(rows []line, top float64, p profile) map[string]int64 {
	var values []Word
	for _, row := range rows {
		if row.top-top > p.SummaryRowHeight {
			break
		}
		for _, word := range row.words {
			if p.SummaryMaxX > 0 && word.X0 >= p.SummaryMaxX {
				continue
			}
			raw, hasPrefix := strings.CutPrefix(word.Text, p.SummaryCurrencyPrefix)
			if p.RoundedCardDue && !hasPrefix {
				raw, hasPrefix = word.Text, true
			}
			if hasPrefix && raw != "" && ((raw[0] >= '0' && raw[0] <= '9') || raw[0] == '-') {
				values = append(values, word)
			}
		}
	}
	if len(values) != len(p.SummaryOrder) {
		return nil
	}
	sort.Slice(values, func(i, j int) bool { return values[i].X0 < values[j].X0 })
	out := make(map[string]int64, len(values))
	for i, word := range values {
		value, ok := p.money(strings.TrimPrefix(word.Text, p.SummaryCurrencyPrefix))
		if !ok {
			return nil
		}
		out[p.SummaryOrder[i]] = value
	}
	return out
}

func readLabeledCardTotals(pages [][]line, p profile) map[string]int64 {
	out := make(map[string]int64, len(p.SummaryLabels))
	for _, row := range pages[0] {
		var fields []string
		for _, word := range row.words {
			if word.X0 < p.SummaryMaxX {
				fields = append(fields, word.Text)
			}
		}
		text := strings.ToLower(strings.Join(fields, " "))
		for i, label := range p.SummaryLabels {
			if !strings.HasPrefix(text, label+" ") {
				continue
			}
			for _, field := range fields {
				value, ok := p.money(strings.TrimPrefix(field, p.SummaryCurrencyPrefix))
				if ok {
					key := p.SummaryOrder[i]
					if _, exists := out[key]; exists {
						return nil
					}
					out[key] = value
					break
				}
			}
		}
	}
	if len(out) != len(p.SummaryLabels) {
		return nil
	}
	return out
}

func (p profile) cardAmount(raw string) (int64, string, bool) {
	if p.SignConvention == "credit_prefix" {
		direction := "debit"
		if rest, ok := strings.CutPrefix(raw, "+"); ok {
			direction, raw = "credit", strings.TrimSpace(rest)
		}
		raw, ok := strings.CutPrefix(raw, p.SummaryCurrencyPrefix)
		if !ok {
			return 0, "", false
		}
		raw = strings.TrimSpace(strings.TrimRight(strings.TrimSpace(raw), "l|"))
		amount, valid := p.money(raw)
		return amount, direction, valid && amount > 0 && amount <= 999_999_999_999
	}
	parts := strings.Fields(raw)
	if len(parts) == 0 || len(parts) > 2 {
		return 0, "", false
	}
	direction := "debit"
	if len(parts) == 2 {
		switch strings.ToUpper(parts[1]) {
		case "CR":
			direction = "credit"
		case "DR":
			if p.SignConvention != "direction_suffix" {
				return 0, "", false
			}
		default:
			return 0, "", false
		}
	} else if p.SignConvention == "direction_suffix" {
		return 0, "", false
	}
	amount, valid := p.money(parts[0])
	return amount, direction, valid && amount > 0 && amount <= 999_999_999_999
}

func directionTotals(rows []LedgerRow) (debit, credit int64) {
	for _, row := range rows {
		switch row.Type {
		case "debit":
			debit += row.Amount
		case "credit":
			credit += row.Amount
		}
	}
	return debit, credit
}

func isCardProfile(id string) bool {
	for _, p := range profiles {
		if p.ID == id {
			return p.Layout == "card_summary"
		}
	}
	return false
}
