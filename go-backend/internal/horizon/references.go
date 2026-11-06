package horizon

import (
	"database/sql"
	"net/http"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

func referencesAccessible(w http.ResponseWriter, r *http.Request, q *query.Queries, userID int64, categoryID, budgetID, deductionID, subscriptionID *int64) bool {
	ids := []*int64{categoryID, budgetID, deductionID, subscriptionID}
	var values [4]sql.NullInt64
	hasReferences := false
	for i, id := range ids {
		if id == nil {
			continue
		}
		if *id <= 0 {
			webutil.BadRequest(w, "referenced record is unavailable")
			return false
		}
		hasReferences = true
		values[i] = sql.NullInt64{Int64: *id, Valid: true}
	}
	if !hasReferences {
		return true
	}
	accessible, err := q.ReferencesAccessible(r.Context(), query.ReferencesAccessibleParams{
		UserID: userID, CategoryID: values[0], BudgetID: values[1], DeductionID: values[2], SubscriptionID: values[3],
	})
	if err != nil {
		webutil.ServerError(w, err)
		return false
	}
	if !accessible.Valid || !accessible.Bool {
		webutil.BadRequest(w, "referenced record is unavailable")
		return false
	}
	return true
}
