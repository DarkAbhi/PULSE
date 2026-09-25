package mealplan

import (
	"context"
	"database/sql"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/mealplan/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{queries: query.New(db)} }

type CreateMealPlanParams = query.CreateMealPlanParams
type CreateMealTimeParams = query.CreateMealTimeParams
type CreateMealTimeRow = query.CreateMealTimeRow
type DeleteMealPlanParams = query.DeleteMealPlanParams
type DeleteMealTimeParams = query.DeleteMealTimeParams
type EnsureDefaultMealTimeParams = query.EnsureDefaultMealTimeParams
type GetMealPlanForUpdateParams = query.GetMealPlanForUpdateParams
type GetMealPlanForUpdateRow = query.GetMealPlanForUpdateRow
type GetMealTimeRangeParams = query.GetMealTimeRangeParams
type GetMealTimeRangeRow = query.GetMealTimeRangeRow
type ListMealPlansByDateParams = query.ListMealPlansByDateParams
type ListMealPlansByRangeParams = query.ListMealPlansByRangeParams
type ListMealTimesRow = query.ListMealTimesRow
type SetMealPlanConsumedParams = query.SetMealPlanConsumedParams
type UpdateMealPlanParams = query.UpdateMealPlanParams

type mealPlanRow struct {
	ID           int64
	Date         string
	Name         string
	MealTimeID   sql.NullInt64
	MealTimeName sql.NullString
	StartTime    string
	EndTime      string
	IsConsumed   bool
	CreatedAt    time.Time
}
type CreateMealPlanRow = mealPlanRow
type ListMealPlansByDateRow = mealPlanRow
type ListMealPlansByRangeRow = mealPlanRow
type ListRecentMealPlansRow = mealPlanRow
type SetMealPlanConsumedRow = mealPlanRow
type UpdateMealPlanRow = mealPlanRow

func makeMealPlanRow(id int64, date, name string, mealTimeID sql.NullInt64, mealTimeName sql.NullString, start, end string, consumed bool, created pgtype.Timestamptz) mealPlanRow {
	return mealPlanRow{ID: id, Date: date, Name: name, MealTimeID: mealTimeID, MealTimeName: mealTimeName, StartTime: start, EndTime: end, IsConsumed: consumed, CreatedAt: created.Time}
}
func (r *Repository) CreateMealPlan(ctx context.Context, arg CreateMealPlanParams) (CreateMealPlanRow, error) {
	row, err := r.queries.CreateMealPlan(ctx, arg)
	return makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
func (r *Repository) CreateMealTime(ctx context.Context, arg CreateMealTimeParams) (CreateMealTimeRow, error) {
	return r.queries.CreateMealTime(ctx, arg)
}
func (r *Repository) DeleteMealPlan(ctx context.Context, arg DeleteMealPlanParams) (int64, error) {
	return r.queries.DeleteMealPlan(ctx, arg)
}
func (r *Repository) DeleteMealTime(ctx context.Context, arg DeleteMealTimeParams) (int64, error) {
	return r.queries.DeleteMealTime(ctx, arg)
}
func (r *Repository) EnsureDefaultMealTime(ctx context.Context, arg EnsureDefaultMealTimeParams) error {
	return r.queries.EnsureDefaultMealTime(ctx, arg)
}
func (r *Repository) GetMealPlanForUpdate(ctx context.Context, arg GetMealPlanForUpdateParams) (GetMealPlanForUpdateRow, error) {
	return r.queries.GetMealPlanForUpdate(ctx, arg)
}
func (r *Repository) GetMealTimeRange(ctx context.Context, arg GetMealTimeRangeParams) (GetMealTimeRangeRow, error) {
	return r.queries.GetMealTimeRange(ctx, arg)
}
func (r *Repository) ListMealPlansByDate(ctx context.Context, arg ListMealPlansByDateParams) ([]ListMealPlansByDateRow, error) {
	rows, err := r.queries.ListMealPlansByDate(ctx, arg)
	if err != nil {
		return nil, err
	}
	out := make([]ListMealPlansByDateRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) ListMealPlansByRange(ctx context.Context, arg ListMealPlansByRangeParams) ([]ListMealPlansByRangeRow, error) {
	rows, err := r.queries.ListMealPlansByRange(ctx, arg)
	if err != nil {
		return nil, err
	}
	out := make([]ListMealPlansByRangeRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) ListMealTimes(ctx context.Context, userID sql.NullInt64) ([]ListMealTimesRow, error) {
	return r.queries.ListMealTimes(ctx, userID)
}
func (r *Repository) ListRecentMealPlans(ctx context.Context, userID int64) ([]ListRecentMealPlansRow, error) {
	rows, err := r.queries.ListRecentMealPlans(ctx, userID)
	if err != nil {
		return nil, err
	}
	out := make([]ListRecentMealPlansRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) SetMealPlanConsumed(ctx context.Context, arg SetMealPlanConsumedParams) (SetMealPlanConsumedRow, error) {
	row, err := r.queries.SetMealPlanConsumed(ctx, arg)
	return makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
func (r *Repository) UpdateMealPlan(ctx context.Context, arg UpdateMealPlanParams) (UpdateMealPlanRow, error) {
	row, err := r.queries.UpdateMealPlan(ctx, arg)
	return makeMealPlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
