package statementimport

import (
	"embed"
	"encoding/json"
	"regexp"
	"slices"
	"sort"
	"strconv"
	"strings"
	"time"
)

//go:embed profiles/*.json
var profilesFS embed.FS

type Word struct {
	Text string  `json:"text"`
	X0   float64 `json:"x0"`
	X1   float64 `json:"x1"`
	Top  float64 `json:"top"`
}

type Table struct {
	BBox      [4]float64   `json:"bbox"`
	RowBounds [][4]float64 `json:"row_bounds"`
}

type Page struct {
	Words  []Word  `json:"words"`
	Tables []Table `json:"tables"`
}

type Document struct {
	PageCount int    `json:"page_count"`
	OCRUsed   bool   `json:"ocr_used"`
	Pages     []Page `json:"pages"`
}

type profile struct {
	SummaryOrder          []string  `json:"summary_order"`
	SummaryCurrencyPrefix string    `json:"summary_currency_prefix"`
	SummaryRowHeight      float64   `json:"summary_row_height"`
	SummaryMaxX           float64   `json:"summary_max_x"`
	SummaryLabels         []string  `json:"summary_labels"`
	DescriptionColumn     int       `json:"description_column"`
	RoundedCardDue        bool      `json:"rounded_card_due"`
	WholeAmounts          bool      `json:"whole_amounts"`
	OpeningPlaceholders   bool      `json:"opening_placeholders"`
	Layout                string    `json:"layout"`
	RegionPrefix          string    `json:"region_prefix"`
	ID                    string    `json:"id"`
	Institution           []string  `json:"institution"`
	AccountType           string    `json:"account_type"`
	AccountSubtype        string    `json:"account_subtype"`
	Detection             []string  `json:"detection"`
	CurrencyMarker        string    `json:"currency_marker"`
	Currency              string    `json:"currency"`
	DateFormat            string    `json:"date_format"`
	DecimalSep            string    `json:"decimal_separator"`
	GroupSep              string    `json:"group_separator"`
	LineTolerance         float64   `json:"line_tolerance"`
	Anchors               []string  `json:"header_anchors"`
	Offsets               []float64 `json:"column_offsets"`
	SummaryHeader         string    `json:"summary_header"`
	SummaryPrefix         string    `json:"summary_row_prefix"`
	OpeningLabel          string    `json:"opening_label"`
	AccountPattern        string    `json:"account_number_pattern"`
	ContinuationGap       float64   `json:"continuation_gap"`
	FooterPattern         string    `json:"footer_pattern"`
	SignConvention        string    `json:"sign_convention"`
}

func loadProfiles() []profile {
	files, err := profilesFS.ReadDir("profiles")
	if err != nil {
		panic(err)
	}
	result := make([]profile, 0, len(files))
	for _, file := range files {
		data, err := profilesFS.ReadFile("profiles/" + file.Name())
		if err != nil {
			panic(err)
		}
		var p profile
		if err := json.Unmarshal(data, &p); err != nil {
			panic(err)
		}
		result = append(result, p)
	}
	return result
}

var profiles = loadProfiles()

type LedgerRow struct {
	Date            string `json:"date"`
	Description     string `json:"description"`
	Amount          int64  `json:"amount_minor"`
	Type            string `json:"type"`
	Balance         int64  `json:"balance_minor"`
	Reference       string `json:"reference,omitempty"`
	StableReference string `json:"stable_reference,omitempty"`
}

type Ledger struct {
	Profile  profile
	LastFour string
	Opening  int64
	Closing  int64
	Rows     []LedgerRow
}

type line struct {
	words []Word
	text  string
	top   float64
}

func lines(page Page, tolerance float64) []line {
	words := slices.Clone(page.Words)
	sort.SliceStable(words, func(i, j int) bool { return words[i].Top < words[j].Top })
	var rows []line
	for _, word := range words {
		if len(rows) == 0 || word.Top-rows[len(rows)-1].top > tolerance {
			rows = append(rows, line{top: word.Top})
		}
		last := &rows[len(rows)-1]
		last.words = append(last.words, word)
	}
	for i := range rows {
		row := &rows[i]
		sort.SliceStable(row.words, func(i, j int) bool { return row.words[i].X0 < row.words[j].X0 })
		parts := make([]string, len(row.words))
		for j, word := range row.words {
			parts[j] = word.Text
		}
		row.text = strings.Join(parts, " ")
	}
	return rows
}

func parseDocument(doc Document) (Ledger, *failure) {
	var detected *profile
	var detectedLines [][]line
	var text string
	for _, p := range profiles {
		var pages [][]line
		var all strings.Builder
		for _, page := range doc.Pages {
			rows := lines(page, p.LineTolerance)
			pages = append(pages, rows)
			for _, row := range rows {
				all.WriteString(strings.ToLower(row.text))
				all.WriteByte('\n')
			}
		}
		if slices.ContainsFunc(p.Detection, func(marker string) bool { return !strings.Contains(all.String(), marker) }) {
			continue
		}
		if detected != nil {
			return Ledger{}, unsupported
		}
		detected, detectedLines, text = &p, pages, all.String()
	}
	if detected == nil {
		return Ledger{}, unsupported
	}
	p := *detected
	if !strings.Contains(text, p.CurrencyMarker) {
		return Ledger{Profile: p}, currencyMismatch
	}
	switch p.Layout {
	case "ruled_grid":
		return assembleGrid(doc.Pages, p)
	case "card_summary":
		return assembleCard(detectedLines, p)
	case "bank_running_balance":
		return assembleRunningBalance(detectedLines, p)
	default:
		return assemble(detectedLines, p)
	}
}

func (p profile) boundaries(row line) []float64 {
	var cuts []float64
	for _, word := range row.words {
		if len(cuts) < len(p.Anchors) && strings.HasPrefix(strings.ToLower(word.Text), p.Anchors[len(cuts)]) {
			cuts = append(cuts, word.X0+p.Offsets[len(cuts)])
		}
	}
	if len(cuts) == len(p.Anchors) && slices.IsSorted(cuts) {
		return cuts
	}
	return nil
}

func columns(row line, cuts []float64) [7]string {
	var builders [7]strings.Builder
	for _, word := range row.words {
		i := sort.SearchFloat64s(cuts, (word.X0+word.X1)/2)
		if builders[i].Len() > 0 {
			builders[i].WriteByte(' ')
		}
		builders[i].WriteString(word.Text)
	}
	var out [7]string
	for i := range builders {
		out[i] = builders[i].String()
	}
	return out
}

func assemble(pages [][]line, p profile) (Ledger, *failure) {
	result := Ledger{Profile: p}
	accountRe := regexp.MustCompile(p.AccountPattern)
	footer := regexp.MustCompile(p.FooterPattern)
	var hasSummary, hasOpening bool
	var opening int64
	for _, page := range pages {
		var cuts []float64
		var lastTop float64
		for i, row := range page {
			text := strings.ToLower(row.text)
			if id := accountRe.FindStringSubmatch(row.text); len(id) > 1 {
				if result.LastFour != "" && result.LastFour != id[1] {
					return result, accountMismatch
				}
				result.LastFour = id[1]
			}
			if text == p.SummaryHeader && i+1 < len(page) {
				next := page[i+1]
				fields := strings.Fields(next.text)
				if !strings.HasPrefix(strings.ToLower(next.text), p.SummaryPrefix) || len(fields) < 4 || hasSummary {
					return result, balancesUnavailable
				}
				var ok bool
				result.Opening, ok = p.money(fields[len(fields)-2])
				if !ok {
					return result, balancesUnavailable
				}
				result.Closing, ok = p.money(fields[len(fields)-1])
				if !ok {
					return result, balancesUnavailable
				}
				hasSummary = true
				cuts = nil
				continue
			}
			if header := p.boundaries(row); len(header) == 6 {
				cuts = header
				lastTop = 0
				continue
			}
			if cuts == nil {
				continue
			}
			if footer.MatchString(text) {
				cuts = nil
				continue
			}
			c := columns(row, cuts)
			if strings.EqualFold(c[2], p.OpeningLabel) {
				var ok bool
				opening, ok = p.money(c[6])
				if !ok || hasOpening || len(result.Rows) > 0 {
					return result, balancesUnavailable
				}
				hasOpening = true
				continue
			}
			if c[0] == "" && c[1] == "" && c[4] == "" && c[5] == "" && c[6] == "" {
				if lastTop > 0 && row.top-lastTop <= p.ContinuationGap {
					item := &result.Rows[len(result.Rows)-1]
					item.Description = strings.TrimSpace(item.Description + " " + c[2])
					item.Reference = strings.TrimSpace(item.Reference + " " + c[3])
					lastTop = row.top
				}
				continue
			}
			ordinal, indexError := strconv.Atoi(c[0])
			date, dateError := time.Parse(p.DateFormat, c[1])
			balance, valid := p.money(c[6])
			if indexError != nil || dateError != nil || !valid || ordinal != len(result.Rows)+1 ||
				c[2] == "" || len(result.Rows) >= 10_000 {
				return result, invalidRow
			}
			direction, field := "debit", c[4]
			if c[4] == "" || c[4] == "-" {
				direction, field = "credit", c[5]
			} else if c[5] != "" && c[5] != "-" {
				return result, invalidRow
			}
			amount, valid := p.money(field)
			if !valid || amount <= 0 || amount > 999_999_999_999 {
				return result, invalidRow
			}
			result.Rows = append(result.Rows, LedgerRow{
				Date: date.Format(time.DateOnly), Description: c[2], Amount: amount, Type: direction, Balance: balance, Reference: c[3],
			})
			lastTop = row.top
		}
	}
	if !hasSummary || !hasOpening || len(result.Rows) == 0 {
		return result, balancesUnavailable
	}
	if opening != result.Opening || !reconciles(result.Opening, result.Closing, result.Rows) {
		return result, reconciliationFailed
	}
	return result, nil
}

var westernGrouping = regexp.MustCompile(`^[0-9]{1,3}(,[0-9]{3})+$`)
var indianGrouping = regexp.MustCompile(`^[0-9]{1,2}(,[0-9]{2})+,[0-9]{3}$`)
var digits = regexp.MustCompile(`^[0-9]+$`)

func (p profile) money(raw string) (int64, bool) {
	raw = strings.TrimSpace(raw)
	negative := strings.HasPrefix(raw, "-")
	if negative {
		raw = raw[1:]
	}
	parts := strings.Split(raw, p.DecimalSep)
	if len(parts) != 2 || len(parts[1]) != 2 || !digits.MatchString(parts[1]) {
		return 0, false
	}
	whole := parts[0]
	if p.GroupSep != "" && strings.Contains(whole, p.GroupSep) {
		grouped := strings.ReplaceAll(whole, p.GroupSep, ",")
		if !westernGrouping.MatchString(grouped) && !indianGrouping.MatchString(grouped) {
			return 0, false
		}
		whole = strings.ReplaceAll(whole, p.GroupSep, "")
	}
	if !digits.MatchString(whole) {
		return 0, false
	}
	amount, err := strconv.ParseInt(whole+parts[1], 10, 64)
	if negative {
		amount = -amount
	}
	return amount, err == nil && amount >= -999_999_999_999_999_999 && amount <= 999_999_999_999_999_999
}

func reconciles(opening, closing int64, rows []LedgerRow) bool {
	for _, row := range rows {
		switch row.Type {
		case "credit":
			opening += row.Amount
		case "debit":
			opening -= row.Amount
		default:
			return false
		}
		if opening != row.Balance {
			return false
		}
	}
	return opening == closing
}
