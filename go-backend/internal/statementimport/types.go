package statementimport

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
)

type rowPreview struct {
	ID                int32    `json:"id"`
	Date              string   `json:"date"`
	Description       string   `json:"description"`
	OriginalNarration string   `json:"original_narration"`
	Amount            string   `json:"amount"`
	Type              string   `json:"type"`
	CategoryChanged   bool     `json:"category_changed,omitempty"`
	CategoryID        *int64   `json:"category_id"`
	Included          bool     `json:"included"`
	Duplicate         string   `json:"duplicate_status"`
	Reconciled        string   `json:"reconciliation_status"`
	Warnings          []string `json:"warnings"`
}

type batchSummary struct {
	ID             string            `json:"id"`
	AccountID      int64             `json:"account_id"`
	Status         string            `json:"status"`
	ProfileID      string            `json:"profile_id"`
	Currency       string            `json:"currency"`
	Revision       int32             `json:"revision"`
	OCRUsed        bool              `json:"ocr_used"`
	ConfirmAccount bool              `json:"account_confirmation_required"`
	ExpiresAt      time.Time         `json:"expires_at"`
	Reconciliation map[string]string `json:"reconciliation"`
	Counts         map[string]int32  `json:"counts"`
}

func summary(b query.StatementImportBatch) batchSummary {
	reconciliation := "unavailable"
	if b.Status == "ready" || b.Status == "committed" {
		reconciliation = "passed"
	} else if b.ReasonCode == reconciliationFailed.code {
		reconciliation = "failed"
	}
	newCount := max(0, b.ParsedCount-b.DuplicateCount-b.ExcludedCount)
	if b.Status == "committed" {
		newCount = b.ImportedCount
	}
	return batchSummary{
		ID: b.ID.String(), AccountID: b.AccountID, Status: b.Status, ProfileID: b.ProfileID, Currency: b.Currency,
		Revision: b.Revision, OCRUsed: b.OcrUsed, ConfirmAccount: !b.AccountConfirmed, ExpiresAt: b.ExpiresAt.Time,
		Reconciliation: map[string]string{"status": reconciliation, "code": b.ReasonCode},
		Counts:         map[string]int32{"parsed": b.ParsedCount, "duplicates": b.DuplicateCount, "new": newCount, "excluded": b.ExcludedCount},
	}
}

type previewPage struct {
	Rows     []rowPreview `json:"rows"`
	Page     int32        `json:"page"`
	PageSize int32        `json:"page_size"`
	Total    int32        `json:"total"`
}

type commitResult struct {
	BatchID    string `json:"batch_id"`
	Status     string `json:"status"`
	Imported   int32  `json:"imported"`
	Duplicates int32  `json:"duplicates_skipped"`
	Excluded   int32  `json:"excluded"`
}

func committed(b query.StatementImportBatch) commitResult {
	return commitResult{b.ID.String(), b.Status, b.ImportedCount, b.DuplicateCount, b.ExcludedCount}
}

type editableRow struct {
	ID          int32           `json:"id"`
	Date        *string         `json:"date"`
	Description *string         `json:"description"`
	Amount      *string         `json:"amount"`
	Type        *string         `json:"type"`
	CategoryID  json.RawMessage `json:"category_id"`
	Included    *bool           `json:"included"`
}

type editInput struct {
	Revision         int32         `json:"revision"`
	AccountConfirmed *bool         `json:"account_confirmed"`
	Changes          []editableRow `json:"changes"`
}

func decimal(amount int64) string {
	return fmt.Sprintf("%d.%02d", amount/100, amount%100)
}

func sourceFingerprint(row LedgerRow) string {
	// Fingerprint the immutable source row so preview edits do not alter its import identity.
	normalized := strings.ToLower(strings.Join(strings.Fields(row.Description), " "))
	return digest(fmt.Appendf(nil, "%s|%s|%d|%d|%s|%s", row.Date, row.Type, row.Amount, row.Balance, row.StableReference, normalized))
}

func digest(raw []byte) string {
	sum := sha256.Sum256(raw)
	return hex.EncodeToString(sum[:])
}

func legacyMerchantKey(description string) string {
	// Reference numbers and payment IDs vary even where the merchant does not.
	parts := strings.FieldsFunc(strings.ToLower(description), func(r rune) bool {
		return (r < 'a' || r > 'z') && (r < '0' || r > '9')
	})
	var terms []string
	for _, part := range parts {
		if !strings.ContainsAny(part, "0123456789") && len(terms) < 8 {
			terms = append(terms, part)
		}
	}
	if len(terms) == 0 {
		return ""
	}
	return digest([]byte(strings.Join(terms, " ")))
}
