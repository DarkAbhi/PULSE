package horizon

import (
	"database/sql"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"net/mail"
	"strconv"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type FinancialAccountInput struct {
	DisplayName        string   `json:"display_name"`
	InstitutionName    string   `json:"institution_name"`
	AccountType        string   `json:"account_type"`
	BankAccountSubtype string   `json:"bank_account_subtype"`
	LastFour           string   `json:"last_four"`
	AccountNumber      string   `json:"account_number"`
	HolderName         string   `json:"holder_name"`
	HolderEmail        string   `json:"holder_email"`
	CardType           string   `json:"card_type"`
	CardNetwork        string   `json:"card_network"`
	CreditLimit        *float64 `json:"credit_limit"`
	StatementDay       *int32   `json:"statement_day"`
}

func normalizeFinancialAccount(in FinancialAccountInput) (FinancialAccountInput, error) {
	in.DisplayName = strings.TrimSpace(in.DisplayName)
	in.InstitutionName = strings.TrimSpace(in.InstitutionName)
	in.AccountType = strings.TrimSpace(in.AccountType)
	in.BankAccountSubtype = strings.TrimSpace(in.BankAccountSubtype)
	in.LastFour = strings.TrimSpace(in.LastFour)
	in.AccountNumber = strings.TrimSpace(in.AccountNumber)
	in.HolderName = strings.TrimSpace(in.HolderName)
	in.HolderEmail = strings.TrimSpace(in.HolderEmail)
	in.CardType = strings.TrimSpace(in.CardType)
	in.CardNetwork = strings.ToLower(strings.TrimSpace(in.CardNetwork))
	if !accountLabelValid(in.DisplayName, 100) || !accountLabelValid(in.InstitutionName, 150) || !accountLabelValid(in.HolderName, 150) {
		return in, errors.New("account, institution and holder names are required and must fit the allowed length")
	}
	if in.LastFour != "" && (len(in.LastFour) != 4 || !asciiBetween(in.LastFour, '0', '9')) {
		return in, errors.New("enter only the last four digits")
	}
	switch in.AccountType {
	case "bank_account":
		if in.BankAccountSubtype != "savings" && in.BankAccountSubtype != "current" {
			return in, errors.New("choose a savings or current account")
		}
		email, err := mail.ParseAddress(in.HolderEmail)
		if err != nil || email.Address != in.HolderEmail || !accountLabelValid(in.HolderEmail, 254) {
			return in, errors.New("enter a valid account holder email")
		}
		if in.AccountNumber != "" {
			if len(in.AccountNumber) < 4 || len(in.AccountNumber) > 34 || strings.IndexFunc(in.AccountNumber, func(r rune) bool { return !(r >= '0' && r <= '9' || r >= 'A' && r <= 'Z' || r >= 'a' && r <= 'z') }) != -1 {
				return in, errors.New("account number must contain 4 to 34 letters or digits")
			}
			suffix := in.AccountNumber[len(in.AccountNumber)-4:]
			if asciiBetween(suffix, '0', '9') {
				in.LastFour = suffix
			} else {
				in.LastFour = ""
			}
		}
		if in.CardType != "" || in.CardNetwork != "" || in.CreditLimit != nil || in.StatementDay != nil {
			return in, errors.New("bank accounts cannot have credit card details")
		}
	case "credit_card":
		if in.BankAccountSubtype != "" || in.AccountNumber != "" || in.HolderEmail != "" {
			return in, errors.New("credit cards cannot have bank account details")
		}
		if !accountLabelValid(in.CardType, 150) || in.LastFour == "" {
			return in, errors.New("card type and last four digits are required")
		}
		switch in.CardNetwork {
		case "visa", "mastercard", "american_express", "diners_club", "rupay":
		default:
			return in, errors.New("choose a supported card network")
		}
		if in.CreditLimit == nil || math.IsNaN(*in.CreditLimit) || math.IsInf(*in.CreditLimit, 0) || *in.CreditLimit < 0 || *in.CreditLimit >= 1e15 {
			return in, errors.New("enter a valid non-negative credit limit below 1000000000000000")
		}
		if in.StatementDay == nil || *in.StatementDay < 1 || *in.StatementDay > 31 {
			return in, errors.New("statement day must be between 1 and 31")
		}
	default:
		return in, errors.New("choose a bank account or credit card")
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

func financialAccountDTO(a query.FinancialAccountDetail) FinancialAccountDTO {
	dto := FinancialAccountDTO{
		ID: a.ID,
		FinancialAccountInput: FinancialAccountInput{
			DisplayName: a.DisplayName, InstitutionName: a.InstitutionName,
			AccountType: a.AccountType, BankAccountSubtype: a.BankAccountSubtype,
			LastFour: a.LastFour, AccountNumber: a.AccountNumber, HolderName: a.HolderName, HolderEmail: a.HolderEmail,
			CardType: a.CardType, CardNetwork: a.CardNetwork, CreditLimit: a.CreditLimit, StatementDay: nullStatementDay(a.StatementDay),
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
	in, err := normalizeFinancialAccount(in)
	if err != nil {
		webutil.BadRequest(w, err.Error())
		return
	}

	tx, err := h.DB.Begin(ctx)
	if err != nil {
		accountError(w, err)
		return
	}
	defer tx.Rollback(ctx)
	q := query.New(tx)
	status := http.StatusOK
	if id == 0 {
		id, err = q.CreateFinancialAccount(ctx, query.CreateFinancialAccountParams{UserID: userID, DisplayName: in.DisplayName, AccountType: in.AccountType})
		status = http.StatusCreated
	} else {
		current, lockErr := q.LockFinancialAccount(ctx, query.LockFinancialAccountParams{ID: id, UserID: userID})
		if lockErr != nil {
			accountError(w, lockErr)
			return
		}
		if current.AccountType != in.AccountType || current.InstitutionName != in.InstitutionName || current.LastFour != in.LastFour ||
			current.BankAccountSubtype != in.BankAccountSubtype || (current.AccountNumber != "" && current.AccountNumber != in.AccountNumber) {
			used, checkErr := q.FinancialAccountHasTransactions(ctx, query.FinancialAccountHasTransactionsParams{AccountID: sql.NullInt64{Int64: id, Valid: true}, UserID: userID})
			if checkErr != nil {
				accountError(w, checkErr)
				return
			}
			if used {
				webutil.WriteJSON(w, http.StatusConflict, map[string]string{"error": "account identity is locked once transactions are linked"})
				return
			}
		}
		_, err = q.UpdateFinancialAccount(ctx, query.UpdateFinancialAccountParams{ID: id, UserID: userID, DisplayName: in.DisplayName, AccountType: in.AccountType})
	}
	if err != nil {
		accountError(w, err)
		return
	}
	if in.AccountType == "bank_account" {
		err = q.DeleteCreditCardDetails(ctx, id)
		if err == nil {
			err = q.SaveBankAccountDetails(ctx, query.SaveBankAccountDetailsParams{AccountID: id, InstitutionName: in.InstitutionName, AccountNumber: in.AccountNumber, HolderName: in.HolderName, HolderEmail: in.HolderEmail, BankAccountSubtype: in.BankAccountSubtype, LastFour: in.LastFour})
		}
	} else {
		err = q.DeleteBankAccountDetails(ctx, id)
		if err == nil {
			err = q.SaveCreditCardDetails(ctx, query.SaveCreditCardDetailsParams{AccountID: id, InstitutionName: in.InstitutionName, CardType: in.CardType, CardNetwork: in.CardNetwork, LastFour: in.LastFour, HolderName: in.HolderName, CreditLimit: *in.CreditLimit, StatementDay: *in.StatementDay})
		}
	}
	if err != nil {
		accountError(w, err)
		return
	}
	row, err := q.GetFinancialAccount(ctx, query.GetFinancialAccountParams{ID: id, UserID: userID})
	if err == nil {
		err = tx.Commit(ctx)
	}
	writeFinancialAccount(w, status, row, err)
}

func nullStatementDay(day sql.NullInt32) *int32 {
	if !day.Valid {
		return nil
	}
	return &day.Int32
}

func (h *Handler) setFinancialAccountArchived(archived bool) func(http.ResponseWriter, *http.Request, int64) {
	return func(w http.ResponseWriter, r *http.Request, userID int64) {
		id, ok := webutil.ParseID(w, r)
		if !ok {
			return
		}
		_, err := query.New(h.DB).SetFinancialAccountArchived(r.Context(), query.SetFinancialAccountArchivedParams{
			ID: id, UserID: userID, Archived: archived,
		})
		if err != nil {
			accountError(w, err)
			return
		}
		row, err := query.New(h.DB).GetFinancialAccount(r.Context(), query.GetFinancialAccountParams{ID: id, UserID: userID})
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

func writeFinancialAccount(w http.ResponseWriter, status int, row query.FinancialAccountDetail, err error) {
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
