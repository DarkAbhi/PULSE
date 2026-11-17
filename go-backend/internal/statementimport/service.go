package statementimport

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"log/slog"
	"math/big"
	"strings"
	"time"
	"unicode/utf8"

	horizonquery "github.com/DarkAbhi/life-backend/internal/horizon/query"
	profilequery "github.com/DarkAbhi/life-backend/internal/profile/query"
	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Service struct {
	pool      *pgxpool.Pool
	extractor extractor
}

func dbFailure(ctx context.Context, err error) *failure {
	if errors.Is(err, pgx.ErrNoRows) {
		return notFound
	}
	// Driver errors may include offending column values.
	slog.ErrorContext(ctx, "statement import database operation failed")
	return internal
}

func (s *Service) account(ctx context.Context, db query.DBTX, user, id int64, lock bool) (horizonquery.FinancialAccount, *failure) {
	q := horizonquery.New(db)
	var a horizonquery.FinancialAccount
	var err error
	if lock {
		a, err = q.LockFinancialAccount(ctx, horizonquery.LockFinancialAccountParams{ID: id, UserID: user})
	} else {
		a, err = q.GetFinancialAccount(ctx, horizonquery.GetFinancialAccountParams{ID: id, UserID: user})
	}
	if err != nil {
		return a, dbFailure(ctx, err)
	}
	if a.ArchivedAt.Valid {
		return a, archived
	}
	currency, err := profilequery.New(db).GetCurrency(ctx, user)
	if err != nil {
		return a, dbFailure(ctx, err)
	}
	if currency != a.Currency {
		return a, currencyMismatch
	}
	return a, nil
}

func (s *Service) upload(ctx context.Context, user, accountID int64, file []byte) (query.StatementImportBatch, *failure) {
	var empty query.StatementImportBatch
	q := query.New(s.pool)
	if _, fail := s.account(ctx, s.pool, user, accountID, false); fail != nil {
		return empty, fail
	}
	source := query.FindSourceBatchParams{UserID: user, AccountID: accountID, SourceSha256: digest(file)}
	if b, fail, found := findSource(ctx, q, source); found {
		return b, fail
	}

	doc, structure, fail := s.extractor.extract(ctx, file)
	file = nil
	var ledger Ledger
	if fail == nil {
		ledger, fail = parseDocument(doc)
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		if fail != nil {
			return empty, fail
		}
		return empty, dbFailure(ctx, err)
	}
	defer tx.Rollback(ctx)
	q = query.New(tx)
	account, accountFailure := s.account(ctx, tx, user, accountID, true)
	if accountFailure != nil {
		return empty, accountFailure
	}
	if b, fail, found := findSource(ctx, q, source); found {
		return b, fail
	}
	confirmed := false
	if fail == nil {
		if account.Currency != ledger.Profile.Currency {
			fail = currencyMismatch
		} else if account.AccountType != ledger.Profile.AccountType ||
			(account.BankAccountSubtype != "" && account.BankAccountSubtype != ledger.Profile.AccountSubtype) ||
			(account.LastFour != "" && ledger.LastFour != "" && account.LastFour != ledger.LastFour) {
			fail = accountMismatch
		} else {
			confirmed = institutionMatch(account.InstitutionName, ledger.Profile) && account.LastFour != "" && ledger.LastFour != ""
		}
	}

	var duplicate []bool
	if fail == nil {
		duplicate, fail = duplicates(ctx, q, user, accountID, source.SourceSha256, ledger.Rows)
		if fail == internal {
			return empty, fail
		}
	}
	params := query.CreateBatchParams{
		ID: pgtype.UUID{Bytes: uuid.New(), Valid: true}, UserID: user, AccountID: accountID, SourceSha256: source.SourceSha256,
		Status: "ready", ProfileID: ledger.Profile.ID, Currency: account.Currency, OcrUsed: doc.OCRUsed,
		AccountConfirmed: confirmed, ParsedCount: int32(len(ledger.Rows)),
		ExpiresAt: pgtype.Timestamptz{Time: time.Now().Add(24 * time.Hour), Valid: true},
	}
	if fail == nil {
		params.OpeningMinor = pgtype.Int8{Int64: ledger.Opening, Valid: true}
		params.ClosingMinor = pgtype.Int8{Int64: ledger.Closing, Valid: true}
		for _, dup := range duplicate {
			if dup {
				params.DuplicateCount++
			}
		}
	} else {
		params.Status, params.ReasonCode = "failed", fail.code
		if fail == unsupported {
			params.Status = "unsupported"
		}
	}

	batch, err := q.CreateBatch(ctx, params)
	if err != nil {
		return empty, dbFailure(ctx, err)
	}
	if fail == nil {
		if err := savePreview(ctx, tx, batch, ledger.Rows, duplicate); err != nil {
			return empty, dbFailure(ctx, err)
		}
	} else if fail == unsupported {
		if err := q.AddStructure(ctx, query.AddStructureParams{BatchID: batch.ID, Document: structure}); err != nil {
			return empty, dbFailure(ctx, err)
		}
	}
	if err := tx.Commit(ctx); err != nil {
		return empty, dbFailure(ctx, err)
	}
	return batch, fail
}

func institutionMatch(name string, p profile) bool {
	name = strings.ToLower(strings.Join(strings.Fields(name), " "))
	for _, alias := range p.Institution {
		if name == alias {
			return true
		}
	}
	return false
}

func findSource(ctx context.Context, q *query.Queries, source query.FindSourceBatchParams) (query.StatementImportBatch, *failure, bool) {
	b, err := q.FindSourceBatch(ctx, source)
	switch {
	case errors.Is(err, pgx.ErrNoRows):
		return b, nil, false
	case err != nil:
		return b, dbFailure(ctx, err), true
	case b.Status == "committed":
		return b, alreadyImported, true
	default:
		return b, nil, true
	}
}

func duplicates(ctx context.Context, q *query.Queries, user, account int64, sourceSHA256 string, rows []LedgerRow) ([]bool, *failure) {
	hashes := make([]string, len(rows))
	counts := make(map[string]int, len(rows))
	for i, row := range rows {
		hashes[i] = sourceFingerprint(row)
		counts[hashes[i]]++
	}
	existing, err := q.ListFingerprints(ctx, query.ListFingerprintsParams{
		UserID: user, AccountID: account, Hashes: hashes, SourceSha256: sourceSHA256,
	})
	if err != nil {
		return nil, dbFailure(ctx, err)
	}
	seen := make(map[string]bool, len(existing))
	for _, prior := range existing {
		if counts[prior.Fingerprint] > 1 || prior.Occurrence > 1 {
			return nil, ambiguousDuplicate
		}
		seen[prior.Fingerprint] = true
	}
	result := make([]bool, len(rows))
	for i, hash := range hashes {
		result[i] = seen[hash]
	}
	return result, nil
}

func savePreview(ctx context.Context, tx pgx.Tx, b query.StatementImportBatch, ledger []LedgerRow, dup []bool) error {
	q := query.New(tx)
	categories, err := horizonquery.New(tx).ListCategories(ctx, sql.NullInt64{Int64: b.UserID, Valid: true})
	if err != nil {
		return err
	}
	byName := make(map[string]int64, len(categories))
	available := make(map[int64]bool, len(categories))
	for _, item := range categories {
		byName[strings.ToLower(item.Name)] = item.ID
		available[item.ID] = true
	}
	manual, err := manualMatches(ctx, tx, b, ledger)
	if err != nil {
		return err
	}
	known := map[string]pgtype.Int8{}
	for i, row := range ledger {
		category := inferredCategory(row.Description, byName)
		if key := merchantKey(row.Description); key != "" {
			learned, found := known[key]
			if !found {
				id, err := q.MerchantCategory(ctx, query.MerchantCategoryParams{UserID: b.UserID, MerchantKey: key})
				if err != nil && !errors.Is(err, pgx.ErrNoRows) {
					return err
				}
				learned = pgtype.Int8{Int64: id, Valid: available[id]}
				known[key] = learned
			}
			if learned.Valid {
				category = &learned.Int64
			}
		}
		id := int32(i + 1)
		name := string([]rune(row.Description)[:min(255, utf8.RuneCountInString(row.Description))])
		preview := rowPreview{
			ID: id, Date: row.Date, Description: name, Amount: decimal(row.Amount), Type: row.Type, CategoryID: category,
			Included: !dup[i], Duplicate: "new", Reconciled: "passed", Warnings: []string{},
		}
		if manual[id] && !dup[i] {
			preview.Warnings = append(preview.Warnings, "POSSIBLE_MANUAL_MATCH")
		}
		if dup[i] {
			preview.Duplicate = "skipped"
		}
		source, err := json.Marshal(row)
		if err != nil {
			return err
		}
		edited, err := json.Marshal(preview)
		if err != nil {
			return err
		}
		if err := q.AddRow(ctx, query.AddRowParams{BatchID: b.ID, Ordinal: id, Source: source, Preview: edited}); err != nil {
			return err
		}
	}
	return nil
}

func manualMatches(ctx context.Context, tx pgx.Tx, batch query.StatementImportBatch, ledger []LedgerRow) (map[int32]bool, error) {
	zone, err := profilequery.New(tx).GetTimeZone(ctx, batch.UserID)
	if errors.Is(err, pgx.ErrNoRows) {
		zone = "UTC"
	} else if err != nil {
		return nil, err
	}
	args := query.PossibleManualMatchesParams{
		UserID: batch.UserID, AccountID: batch.AccountID, Timezone: zone,
		Dates: make([]pgtype.Date, len(ledger)), Amounts: make([]pgtype.Numeric, len(ledger)), Directions: make([]string, len(ledger)),
	}
	for i, row := range ledger {
		date, err := time.Parse(time.DateOnly, row.Date)
		if err != nil {
			return nil, err
		}
		args.Dates[i] = pgtype.Date{Time: date, Valid: true}
		args.Amounts[i] = pgtype.Numeric{Int: big.NewInt(row.Amount), Exp: -2, Valid: true}
		args.Directions[i] = row.Type
	}
	matches, err := query.New(tx).PossibleManualMatches(ctx, args)
	if err != nil {
		return nil, err
	}
	out := make(map[int32]bool, len(matches))
	for _, ordinal := range matches {
		out[ordinal] = true
	}
	return out, nil
}

func inferredCategory(desc string, names map[string]int64) *int64 {
	keywords := []struct {
		term string
		name string
	}{
		{"swiggy", "food & dining"}, {"zomato", "food & dining"}, {"uber", "transportation"}, {"ola", "transportation"},
	}
	search := " " + strings.Join(strings.FieldsFunc(strings.ToLower(desc), func(r rune) bool {
		return r < 'a' || r > 'z'
	}), " ") + " "
	for _, rule := range keywords {
		if strings.Contains(search, " "+rule.term+" ") {
			if id, ok := names[rule.name]; ok {
				return &id
			}
		}
	}
	if id, ok := names["other"]; ok {
		return &id
	}
	return nil
}

func storeTransaction(ctx context.Context, tx pgx.Tx, b query.StatementImportBatch, row rowPreview, amount int64, date time.Time, hash string) error {
	category := "Uncategorized"
	if row.CategoryID != nil {
		var err error
		category, err = horizonquery.New(tx).GetUserCategoryName(ctx, horizonquery.GetUserCategoryNameParams{
			ID: *row.CategoryID, UserID: sql.NullInt64{Int64: b.UserID, Valid: true},
		})
		if err != nil {
			return err
		}
	}
	params := query.ImportTransactionParams{
		UserID: b.UserID, AccountID: pgtype.Int8{Int64: b.AccountID, Valid: true},
		Name: row.Description, Amount: pgtype.Numeric{Int: big.NewInt(amount), Exp: -2, Valid: true},
		Type: row.Type, TransactionDate: pgtype.Timestamptz{Time: date, Valid: true}, CategoryName: category,
		ImportBatchID: b.ID, ImportRowOrdinal: pgtype.Int4{Int32: row.ID, Valid: true},
		DedupeHash: pgtype.Text{String: hash, Valid: true},
	}
	if row.CategoryID != nil {
		params.CategoryID = pgtype.Int8{Int64: *row.CategoryID, Valid: true}
	}
	return query.New(tx).ImportTransaction(ctx, params)
}
