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

type CreatePlanParams = query.CreateMealPlanParams
type CreateTimeParams = query.CreateMealTimeParams
type CreateTimeRow = query.CreateMealTimeRow
type DeletePlanParams = query.DeleteMealPlanParams
type DeleteTimeParams = query.DeleteMealTimeParams
type EnsureDefaultTimeParams = query.EnsureDefaultMealTimeParams
type PlanForUpdateParams = query.GetMealPlanForUpdateParams
type PlanForUpdateRow = query.GetMealPlanForUpdateRow
type TimeRangeParams = query.GetMealTimeRangeParams
type TimeRangeRow = query.GetMealTimeRangeRow
type ListPlansByDateParams = query.ListMealPlansByDateParams
type ListPlansByRangeParams = query.ListMealPlansByRangeParams
type ListTimesRow = query.ListMealTimesRow
type SetPlanConsumedParams = query.SetMealPlanConsumedParams
type UpdatePlanParams = query.UpdateMealPlanParams

type planRow struct {
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
type CreatePlanRow = planRow
type ListPlansByDateRow = planRow
type ListPlansByRangeRow = planRow
type ListRecentPlansRow = planRow
type SetPlanConsumedRow = planRow
type UpdatePlanRow = planRow

func makePlanRow(id int64, date, name string, mealTimeID sql.NullInt64, mealTimeName sql.NullString, start, end string, isConsumed bool, created pgtype.Timestamptz) planRow {
	return planRow{ID: id, Date: date, Name: name, MealTimeID: mealTimeID, MealTimeName: mealTimeName, StartTime: start, EndTime: end, IsConsumed: isConsumed, CreatedAt: created.Time}
}
func (r *Repository) CreatePlan(ctx context.Context, arg CreatePlanParams) (CreatePlanRow, error) {
	row, err := r.queries.CreateMealPlan(ctx, arg)
	return makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
func (r *Repository) CreateTime(ctx context.Context, arg CreateTimeParams) (CreateTimeRow, error) {
	return r.queries.CreateMealTime(ctx, arg)
}
func (r *Repository) DeletePlan(ctx context.Context, arg DeletePlanParams) (int64, error) {
	return r.queries.DeleteMealPlan(ctx, arg)
}
func (r *Repository) DeleteTime(ctx context.Context, arg DeleteTimeParams) (int64, error) {
	return r.queries.DeleteMealTime(ctx, arg)
}
func (r *Repository) EnsureDefaultTime(ctx context.Context, arg EnsureDefaultTimeParams) error {
	return r.queries.EnsureDefaultMealTime(ctx, arg)
}
func (r *Repository) FetchPlanForUpdate(ctx context.Context, arg PlanForUpdateParams) (PlanForUpdateRow, error) {
	return r.queries.GetMealPlanForUpdate(ctx, arg)
}
func (r *Repository) FetchTimeRange(ctx context.Context, arg TimeRangeParams) (TimeRangeRow, error) {
	return r.queries.GetMealTimeRange(ctx, arg)
}
func (r *Repository) ListPlansByDate(ctx context.Context, arg ListPlansByDateParams) ([]ListPlansByDateRow, error) {
	rows, err := r.queries.ListMealPlansByDate(ctx, arg)
	if err != nil {
		return nil, err
	}
	out := make([]ListPlansByDateRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) ListPlansByRange(ctx context.Context, arg ListPlansByRangeParams) ([]ListPlansByRangeRow, error) {
	rows, err := r.queries.ListMealPlansByRange(ctx, arg)
	if err != nil {
		return nil, err
	}
	out := make([]ListPlansByRangeRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) ListTimes(ctx context.Context, userID sql.NullInt64) ([]ListTimesRow, error) {
	return r.queries.ListMealTimes(ctx, userID)
}
func (r *Repository) ListRecentPlans(ctx context.Context, userID int64) ([]ListRecentPlansRow, error) {
	rows, err := r.queries.ListRecentMealPlans(ctx, userID)
	if err != nil {
		return nil, err
	}
	out := make([]ListRecentPlansRow, 0, len(rows))
	for _, row := range rows {
		out = append(out, makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
	}
	return out, nil
}
func (r *Repository) SetPlanConsumed(ctx context.Context, arg SetPlanConsumedParams) (SetPlanConsumedRow, error) {
	row, err := r.queries.SetMealPlanConsumed(ctx, arg)
	return makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
func (r *Repository) UpdatePlan(ctx context.Context, arg UpdatePlanParams) (UpdatePlanRow, error) {
	row, err := r.queries.UpdateMealPlan(ctx, arg)
	return makePlanRow(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), err
}
