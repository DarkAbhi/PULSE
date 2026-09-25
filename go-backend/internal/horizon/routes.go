package horizon

import "github.com/go-chi/chi/v5"

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/horizon", h.Summary)
	r.Put("/horizon/config", h.UpdateConfig)
	r.Post("/horizon/budgets", h.CreateBudget)
	r.Put("/horizon/budgets/{id}", h.UpdateBudget)
	r.Delete("/horizon/budgets/{id}", h.DeleteBudget)
	r.Post("/horizon/deductions", h.CreateDeduction)
	r.Put("/horizon/deductions/{id}", h.UpdateDeduction)
	r.Delete("/horizon/deductions/{id}", h.DeleteDeduction)
	r.Get("/horizon/categories", h.ListCategories)
	r.Post("/horizon/categories", h.CreateCategory)
	r.Get("/horizon/transactions", h.ListTransactions)
	r.Post("/horizon/transactions", h.CreateTransaction)
	r.Post("/horizon/transactions/bulk", h.BulkCreateTransactions)
	r.Put("/horizon/transactions/{id}", h.UpdateTransaction)
	r.Delete("/horizon/transactions/{id}", h.DeleteTransaction)
	r.Get("/horizon/subscriptions", h.ListSubscriptions)
	r.Post("/horizon/subscriptions", h.CreateSubscription)
	r.Put("/horizon/subscriptions/{id}", h.UpdateSubscription)
	r.Delete("/horizon/subscriptions/{id}", h.DeleteSubscription)
	r.Get("/horizon/subscriptions/{id}/transactions", h.ListSubscriptionTransactions)
}
