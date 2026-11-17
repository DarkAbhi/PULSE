package statementimport

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"

	horizonquery "github.com/DarkAbhi/life-backend/internal/horizon/query"
	profilequery "github.com/DarkAbhi/life-backend/internal/profile/query"
	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
)

func rowsForReview(records []query.StatementImportRow) ([]rowPreview, []LedgerRow, error) {
	preview := make([]rowPreview, len(records))
	source := make([]LedgerRow, len(records))
	for index, row := range records {
		if err := json.Unmarshal(row.Preview, &preview[index]); err != nil {
			return nil, nil, err
		}
		if err := json.Unmarshal(row.Source, &source[index]); err != nil {
			return nil, nil, err
		}
		preview[index].OriginalNarration = source[index].Description
	}
	return preview, source, nil
}

func ready(b query.StatementImportBatch) *failure {
	if b.Status == "expired" || (b.Status != "committed" && !b.ExpiresAt.Time.After(time.Now())) {
		return expired
	}
	if b.Status != "ready" {
		return notReady
	}
	return nil
}

func applyEdit(row *rowPreview, edit editableRow) *failure {
	if row.Duplicate == "skipped" {
		return invalidRow
	}
	if edit.Date != nil {
		row.Date = *edit.Date
	}
	if edit.Description != nil {
		row.Description = strings.TrimSpace(*edit.Description)
	}
	if edit.Amount != nil {
		row.Amount = *edit.Amount
	}
	if edit.Type != nil {
		row.Type = *edit.Type
	}
	if edit.Included != nil {
		row.Included = *edit.Included
	}
	if edit.CategoryID != nil {
		row.CategoryChanged = true
		if err := json.Unmarshal(edit.CategoryID, &row.CategoryID); err != nil || (row.CategoryID != nil && *row.CategoryID <= 0) {
			return invalidRow
		}
	}
	if row.Description == "" || utf8.RuneCountInString(row.Description) > 255 ||
		strings.IndexFunc(row.Description, unicode.IsControl) >= 0 {
		return invalidRow
	}
	return nil
}

func verifyPreview(b query.StatementImportBatch, source []LedgerRow, preview []rowPreview) ([]LedgerRow, *failure) {
	if len(source) == 0 || len(source) != len(preview) || !b.OpeningMinor.Valid || !b.ClosingMinor.Valid {
		return nil, balancesUnavailable
	}
	normalized := make([]LedgerRow, len(source))
	var previous string
	p := profile{DecimalSep: "."}
	card := isCardProfile(b.ProfileID)
	for i, row := range preview {
		date, err := time.Parse(time.DateOnly, row.Date)
		amount, ok := p.money(row.Amount)
		if err != nil || date.Format(time.DateOnly) != row.Date || (!card && row.Date < previous) || (row.Type != "credit" && row.Type != "debit") ||
			!ok || amount <= 0 || amount > 999_999_999_999 {
			return nil, invalidRow
		}
		normalized[i] = LedgerRow{Date: row.Date, Amount: amount, Type: row.Type, Balance: source[i].Balance}
		previous = row.Date
	}
	if card {
		debit, credit := directionTotals(normalized)
		sourceDebit, sourceCredit := directionTotals(source)
		if debit != sourceDebit || credit != sourceCredit || b.OpeningMinor.Int64+debit-credit != b.ClosingMinor.Int64 {
			return nil, reconciliationFailed
		}
	} else if !reconciles(b.OpeningMinor.Int64, b.ClosingMinor.Int64, normalized) {
		return nil, reconciliationFailed
	}
	return normalized, nil
}

func (s *Service) edit(ctx context.Context, tx pgx.Tx, b query.StatementImportBatch, input editInput) (query.StatementImportBatch, *failure) {
	if fail := ready(b); fail != nil {
		return b, fail
	}
	if input.Revision != b.Revision {
		return b, stale
	}
	q := query.New(tx)
	records, err := q.ListRows(ctx, b.ID)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	rows, source, err := rowsForReview(records)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	changed := make(map[int32]bool, len(input.Changes))
	for _, change := range input.Changes {
		id := change.ID
		if id <= 0 || int(id) > len(rows) || rows[id-1].ID != id || changed[id] {
			return b, invalidRow
		}
		changed[id] = true
		row := &rows[id-1]
		if fail := applyEdit(row, change); fail != nil {
			return b, fail
		}
		if change.CategoryID != nil && row.CategoryID != nil {
			_, err := horizonquery.New(tx).GetUserCategoryName(ctx, horizonquery.GetUserCategoryNameParams{
				ID: *row.CategoryID, UserID: sql.NullInt64{Int64: b.UserID, Valid: true},
			})
			if errors.Is(err, pgx.ErrNoRows) {
				return b, invalidRow
			}
			if err != nil {
				return b, dbFailure(ctx, err)
			}
		}
	}
	if _, fail := verifyPreview(b, source, rows); fail != nil {
		return b, fail
	}
	var excluded int32
	for _, row := range rows {
		if !row.Included && row.Duplicate != "skipped" {
			excluded++
		}
		if changed[row.ID] {
			raw, err := json.Marshal(row)
			if err != nil {
				return b, dbFailure(ctx, err)
			}
			if err := q.UpdateRow(ctx, query.UpdateRowParams{BatchID: b.ID, Ordinal: row.ID, Preview: raw}); err != nil {
				return b, dbFailure(ctx, err)
			}
		}
	}
	confirmed := b.AccountConfirmed
	if input.AccountConfirmed != nil {
		confirmed = confirmed || *input.AccountConfirmed
	}
	b, err = q.UpdateBatch(ctx, query.UpdateBatchParams{ID: b.ID, AccountConfirmed: confirmed, ExcludedCount: excluded})
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	return b, nil
}

func (s *Service) commit(ctx context.Context, tx pgx.Tx, b query.StatementImportBatch, revision int32) (query.StatementImportBatch, *failure) {
	if b.Status == "committed" {
		return b, nil
	}
	if fail := ready(b); fail != nil {
		return b, fail
	}
	if b.Revision != revision {
		return b, stale
	}
	if !b.AccountConfirmed {
		return b, &failure{422, "ACCOUNT_CONFIRMATION_REQUIRED", "Confirm the destination account first."}
	}
	if _, fail := s.account(ctx, tx, b.UserID, b.AccountID, true); fail != nil {
		return b, fail
	}
	q := query.New(tx)
	records, err := q.ListRows(ctx, b.ID)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	rows, source, err := rowsForReview(records)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	normalized, fail := verifyPreview(b, source, rows)
	if fail != nil {
		return b, fail
	}
	skips, fail := duplicates(ctx, q, b.UserID, b.AccountID, b.SourceSha256, source)
	if fail != nil {
		return b, fail
	}
	zone, err := profilequery.New(tx).GetTimeZone(ctx, b.UserID)
	if errors.Is(err, pgx.ErrNoRows) {
		zone = "UTC"
	} else if err != nil {
		return b, dbFailure(ctx, err)
	}
	location, err := time.LoadLocation(zone)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	counts := query.CommitBatchParams{ID: b.ID}
	occurrence := map[string]int32{}
	for i, row := range rows {
		hash := sourceFingerprint(source[i])
		occurrence[hash]++
		switch {
		case skips[i] || row.Duplicate == "skipped":
			counts.DuplicateCount++
			continue
		case !row.Included:
			counts.ExcludedCount++
			continue
		}
		date, err := time.ParseInLocation(time.DateTime, row.Date+" 12:00:00", location)
		if err != nil || date.Format(time.DateOnly) != row.Date {
			return b, invalidRow
		}
		if err := storeTransaction(ctx, tx, b, row, normalized[i].Amount, date, hash); err != nil {
			return b, dbFailure(ctx, err)
		}
		// Under the account lock, a matching fingerprint can only be reused for
		// the same PDF after all transactions in its original batch were deleted.
		if err := q.RecordFingerprint(ctx, query.RecordFingerprintParams{
			UserID: b.UserID, AccountID: b.AccountID, Fingerprint: hash, Occurrence: occurrence[hash], BatchID: b.ID, Ordinal: row.ID,
		}); err != nil {
			return b, dbFailure(ctx, err)
		}
		counts.ImportedCount++
		if row.CategoryID != nil && row.CategoryChanged && row.Type == source[i].Type {
			if key := suggestNarration(source[i]).key; key != "" {
				if err := q.LearnCategory(ctx, query.LearnCategoryParams{UserID: b.UserID, MerchantKey: key, CategoryID: *row.CategoryID}); err != nil {
					return b, dbFailure(ctx, err)
				}
			}
		}
	}
	b, err = q.CommitBatch(ctx, counts)
	if err != nil {
		return b, dbFailure(ctx, err)
	}
	return b, nil
}

func parseBatchID(raw string) (pgtype.UUID, error) {
	var id pgtype.UUID
	err := id.Scan(raw)
	return id, err
}
