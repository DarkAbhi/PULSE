package horizon

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"

	"github.com/DarkAbhi/life-backend/internal/currency"
	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	profilequery "github.com/DarkAbhi/life-backend/internal/profile/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

// FinancialAccountInput deliberately accepts only a masked identifier, never
// account credentials or a complete card/account number.
type FinancialAccountInput struct {
	DisplayName        string `json:"display_name"`
	InstitutionName    string `json:"institution_name"`
	AccountType        string `json:"account_type"`
	BankAccountSubtype string `json:"bank_account_subtype"`
	Currency           string `json:"currency"`
	CountryCode        string `json:"country_code"`
	LastFour           string `json:"last_four"`
}

func normalizeFinancialAccount(in FinancialAccountInput, profileCurrency string) (FinancialAccountInput, error) {
	in.DisplayName = strings.TrimSpace(in.DisplayName)
	in.InstitutionName = strings.TrimSpace(in.InstitutionName)
	in.AccountType = strings.TrimSpace(in.AccountType)
	in.BankAccountSubtype = strings.TrimSpace(in.BankAccountSubtype)
	in.Currency = strings.ToUpper(strings.TrimSpace(in.Currency))
	in.CountryCode = strings.ToUpper(strings.TrimSpace(in.CountryCode))
	in.LastFour = strings.TrimSpace(in.LastFour)
	if !accountLabelValid(in.DisplayName, 100) || !accountLabelValid(in.InstitutionName, 150) {
		return in, errors.New("account and institution names are required and must fit the allowed length")
	}
	if in.AccountType != "bank_account" && in.AccountType != "credit_card" {
		return in, errors.New("choose a bank account or credit card")
	}
	switch in.BankAccountSubtype {
	case "", "savings", "current", "other":
	default:
		return in, errors.New("invalid bank account subtype")
	}
	if in.AccountType == "credit_card" && in.BankAccountSubtype != "" {
		return in, errors.New("credit cards cannot have a bank account subtype")
	}
	if !currency.Valid(in.Currency) || in.Currency != profileCurrency {
		return in, errors.New("account currency must match your configured currency")
	}
	if in.CountryCode != "" && (len(in.CountryCode) != 2 || !asciiBetween(in.CountryCode, 'A', 'Z')) {
		return in, errors.New("country code must contain two letters")
	}
	if in.LastFour != "" && (len(in.LastFour) != 4 || !asciiBetween(in.LastFour, '0', '9')) {
		return in, errors.New("enter only the last four digits")
	}
	return in, nil
}

func accountLabelValid(value string, limit int) bool {
	if value == "" || !utf8.ValidString(value) || utf8.RuneCountInString(value) > limit {
		return false
	}
	return strings.IndexFunc(value, unicode.IsControl) == -1
}

func asciiBetween(value string, low, high byte) bool {
	for i := 0; i < len(value); i++ {
		if value[i] < low || value[i] > high {
			return false
		}
	}
	return true
}

type FinancialAccountDTO struct {
	ID int64 `json:"id"`
	FinancialAccountInput
	ArchivedAt *time.Time `json:"archived_at"`
}

func financialAccountDTO(a query.FinancialAccount) FinancialAccountDTO {
	dto := FinancialAccountDTO{
		ID: a.ID,
		FinancialAccountInput: FinancialAccountInput{
			DisplayName: a.DisplayName, InstitutionName: a.InstitutionName,
			AccountType: a.AccountType, BankAccountSubtype: a.BankAccountSubtype,
			Currency: a.Currency, CountryCode: a.CountryCode, LastFour: a.LastFour,
		},
	}
	if a.ArchivedAt.Valid {
		dto.ArchivedAt = &a.ArchivedAt.Time
	}
	return dto
}

func (h *Handler) listFinancialAccounts(w http.ResponseWriter, r *http.Request, userID int64) {
	includeArchived := false
	if raw := r.URL.Query().Get("include_archived"); raw != "" {
		var err error
		includeArchived, err = strconv.ParseBool(raw)
		if err != nil {
			webutil.BadRequest(w, "invalid include_archived")
			return
		}
	}
	rows, err := query.New(h.DB).ListFinancialAccounts(r.Context(), query.ListFinancialAccountsParams{
		UserID: userID, IncludeArchived: includeArchived,
	})
	if err != nil {
		accountError(w, err)
		return
	}
	accounts := make([]FinancialAccountDTO, 0, len(rows))
	for _, row := range rows {
		accounts = append(accounts, financialAccountDTO(row))
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"accounts": accounts})
}

func (h *Handler) saveFinancialAccount(w http.ResponseWriter, r *http.Request, userID int64) {
	var id int64
	if r.Method == http.MethodPut {
		var ok bool
		id, ok = webutil.ParseID(w, r)
		if !ok {
			return
		}
	}

	var in FinancialAccountInput
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16<<10))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid account details")
		return
	}
	ctx := r.Context()
	profileCurrency, err := profilequery.New(h.DB).GetCurrency(ctx, userID)
	if err != nil {
		accountError(w, err)
		return
	}
	if in.Currency == "" {
		in.Currency = profileCurrency
	}
	in, err = normalizeFinancialAccount(in, profileCurrency)
	if err != nil {
		webutil.BadRequest(w, err.Error())
		return
	}
	if id == 0 {
		row, err := query.New(h.DB).CreateFinancialAccount(ctx, query.CreateFinancialAccountParams{
			UserID: userID, DisplayName: in.DisplayName, InstitutionName: in.InstitutionName,
			AccountType: in.AccountType, BankAccountSubtype: in.BankAccountSubtype,
			Currency: in.Currency, CountryCode: in.CountryCode, LastFour: in.LastFour,
		})
		writeFinancialAccount(w, http.StatusCreated, row, err)
		return
	}

	tx, err := h.DB.Begin(ctx)
	if err != nil {
		accountError(w, err)
		return
	}
	defer tx.Rollback(ctx)
	q := query.New(tx)
	current, err := q.LockFinancialAccount(ctx, query.LockFinancialAccountParams{ID: id, UserID: userID})
	if err != nil {
		accountError(w, err)
		return
	}
	if current.AccountType != in.AccountType || current.Currency != in.Currency ||
		current.InstitutionName != in.InstitutionName || current.LastFour != in.LastFour ||
		current.BankAccountSubtype != in.BankAccountSubtype || current.CountryCode != in.CountryCode {
		used, err := q.FinancialAccountHasTransactions(ctx, query.FinancialAccountHasTransactionsParams{
			AccountID: sql.NullInt64{Int64: id, Valid: true}, UserID: userID,
		})
		if err != nil {
			accountError(w, err)
			return
		}
		if used {
			webutil.WriteJSON(w, http.StatusConflict, map[string]string{"error": "account identity is locked once transactions are linked"})
			return
		}
	}
	row, err := q.UpdateFinancialAccount(ctx, query.UpdateFinancialAccountParams{
		ID: id, UserID: userID, DisplayName: in.DisplayName, InstitutionName: in.InstitutionName,
		AccountType: in.AccountType, BankAccountSubtype: in.BankAccountSubtype,
		Currency: in.Currency, CountryCode: in.CountryCode, LastFour: in.LastFour,
	})
	if err == nil {
		err = tx.Commit(ctx)
	}
	writeFinancialAccount(w, http.StatusOK, row, err)
}

func (h *Handler) setFinancialAccountArchived(archived bool) func(http.ResponseWriter, *http.Request, int64) {
	return func(w http.ResponseWriter, r *http.Request, userID int64) {
		id, ok := webutil.ParseID(w, r)
		if !ok {
			return
		}
		row, err := query.New(h.DB).SetFinancialAccountArchived(r.Context(), query.SetFinancialAccountArchivedParams{
			ID: id, UserID: userID, Archived: archived,
		})
		writeFinancialAccount(w, http.StatusOK, row, err)
	}
}

func (h *Handler) accountRoute(handle func(http.ResponseWriter, *http.Request, int64)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		user, err := h.sessionUser(r)
		if errors.Is(err, sql.ErrNoRows) {
			webutil.Unauthorized(w, "session is invalid or expired")
			return
		}
		if err != nil {
			accountError(w, err)
			return
		}
		handle(w, r, user.ID)
	}
}

func writeFinancialAccount(w http.ResponseWriter, status int, row query.FinancialAccount, err error) {
	if err != nil {
		accountError(w, err)
		return
	}
	webutil.WriteJSON(w, status, financialAccountDTO(row))
}

func accountError(w http.ResponseWriter, err error) {
	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "account not found"})
		return
	}
	webutil.ServerError(w, errors.New("financial account database operation failed"))
}

func transactionAccount(w http.ResponseWriter, r *http.Request, q *query.Queries, userID int64, raw json.RawMessage) (sql.NullInt64, bool) {
	var id *int64
	if raw != nil {
		if err := json.Unmarshal(raw, &id); err != nil || (id != nil && *id <= 0) {
			webutil.BadRequest(w, "account_id must be a positive integer or null")
			return sql.NullInt64{}, false
		}
	}
	if id == nil {
		return sql.NullInt64{}, true
	}
	account, err := q.LockFinancialAccount(r.Context(), query.LockFinancialAccountParams{ID: *id, UserID: userID})
	if err != nil {
		accountError(w, err)
		return sql.NullInt64{}, false
	}
	if account.ArchivedAt.Valid {
		webutil.WriteJSON(w, http.StatusConflict, map[string]string{"error": "account is archived", "code": "ACCOUNT_ARCHIVED"})
		return sql.NullInt64{}, false
	}
	return sql.NullInt64{Int64: account.ID, Valid: true}, true
}
