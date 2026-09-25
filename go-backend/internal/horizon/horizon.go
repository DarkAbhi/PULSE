// Package horizon manages financial plans, budgets, deductions, and transactions.
package horizon

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

func (h *Handler) GetHorizon(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	summary, err := h.service.Summary(r.Context(), user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, summary)
}

func (h *Handler) UpdateConfig(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in ConfigInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	if in.BaseAmount < 0 {
		webutil.BadRequest(w, "base amount cannot be negative")
		return
	}

	currency := "₹"
	if in.Currency != nil && strings.TrimSpace(*in.Currency) != "" {
		currency = strings.TrimSpace(*in.Currency)
	}

	err = h.service.UpsertConfig(r.Context(), user.ID, in.BaseAmount, currency)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	summary, err := h.service.Summary(r.Context(), user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, summary)
}

// Delegate HTTP handlers to sub-package handlers safely

func (h *Handler) CreateBudget(w http.ResponseWriter, r *http.Request) {
	h.Budgets.CreateBudget(w, r)
}

func (h *Handler) UpdateBudget(w http.ResponseWriter, r *http.Request) {
	h.Budgets.UpdateBudget(w, r)
}

func (h *Handler) DeleteBudget(w http.ResponseWriter, r *http.Request) {
	h.Budgets.DeleteBudget(w, r)
}

func (h *Handler) CreateDeduction(w http.ResponseWriter, r *http.Request) {
	h.Deductions.CreateDeduction(w, r)
}

func (h *Handler) UpdateDeduction(w http.ResponseWriter, r *http.Request) {
	h.Deductions.UpdateDeduction(w, r)
}

func (h *Handler) DeleteDeduction(w http.ResponseWriter, r *http.Request) {
	h.Deductions.DeleteDeduction(w, r)
}

func (h *Handler) ListCategories(w http.ResponseWriter, r *http.Request) {
	h.Categories.ListCategories(w, r)
}

func (h *Handler) CreateCategory(w http.ResponseWriter, r *http.Request) {
	h.Categories.CreateCategory(w, r)
}

func (h *Handler) ListTransactions(w http.ResponseWriter, r *http.Request) {
	h.Transactions.ListTransactions(w, r)
}

func (h *Handler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	h.Transactions.CreateTransaction(w, r)
}

func (h *Handler) BulkCreateTransactions(w http.ResponseWriter, r *http.Request) {
	h.Transactions.BulkCreateTransactions(w, r)
}

func (h *Handler) UpdateTransaction(w http.ResponseWriter, r *http.Request) {
	h.Transactions.UpdateTransaction(w, r)
}

func (h *Handler) DeleteTransaction(w http.ResponseWriter, r *http.Request) {
	h.Transactions.DeleteTransaction(w, r)
}

func (h *Handler) ListSubscriptions(w http.ResponseWriter, r *http.Request) {
	h.Subscriptions.ListSubscriptions(w, r)
}

func (h *Handler) CreateSubscription(w http.ResponseWriter, r *http.Request) {
	h.Subscriptions.CreateSubscription(w, r)
}

func (h *Handler) UpdateSubscription(w http.ResponseWriter, r *http.Request) {
	h.Subscriptions.UpdateSubscription(w, r)
}

func (h *Handler) DeleteSubscription(w http.ResponseWriter, r *http.Request) {
	h.Subscriptions.DeleteSubscription(w, r)
}

func (h *Handler) ListSubscriptionTransactions(w http.ResponseWriter, r *http.Request) {
	h.Subscriptions.ListSubscriptionTransactions(w, r)
}
