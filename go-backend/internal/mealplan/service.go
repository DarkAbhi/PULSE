package mealplan

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"
)

var ErrMealNotFound = errors.New("meal not found")
var ErrMealTimeNotFound = errors.New("meal time not found")
var ErrDuplicateMealTime = errors.New("meal time already exists")

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type mealStore interface {
	EnsureDefaultMealTime(context.Context, EnsureDefaultMealTimeParams) error
	ListMealTimes(context.Context, sql.NullInt64) ([]ListMealTimesRow, error)
	CreateMealTime(context.Context, CreateMealTimeParams) (CreateMealTimeRow, error)
	DeleteMealTime(context.Context, DeleteMealTimeParams) (int64, error)
	ListMealPlansByDate(context.Context, ListMealPlansByDateParams) ([]ListMealPlansByDateRow, error)
	ListMealPlansByRange(context.Context, ListMealPlansByRangeParams) ([]ListMealPlansByRangeRow, error)
	ListRecentMealPlans(context.Context, int64) ([]ListRecentMealPlansRow, error)
	GetMealTimeRange(context.Context, GetMealTimeRangeParams) (GetMealTimeRangeRow, error)
	CreateMealPlan(context.Context, CreateMealPlanParams) (CreateMealPlanRow, error)
	DeleteMealPlan(context.Context, DeleteMealPlanParams) (int64, error)
	SetMealPlanConsumed(context.Context, SetMealPlanConsumedParams) (SetMealPlanConsumedRow, error)
	GetMealPlanForUpdate(context.Context, GetMealPlanForUpdateParams) (GetMealPlanForUpdateRow, error)
	UpdateMealPlan(context.Context, UpdateMealPlanParams) (UpdateMealPlanRow, error)
}

type Service struct{ store mealStore }

func NewService(store mealStore) *Service { return &Service{store: store} }

func (s *Service) EnsureDefaults(ctx context.Context) error {
	for _, mt := range defaultMealTimes {
		if err := s.store.EnsureDefaultMealTime(ctx, EnsureDefaultMealTimeParams{Name: mt.Name, Column2: mt.StartTime, Column3: mt.EndTime}); err != nil {
			return fmt.Errorf("ensure default meal time: %w", err)
		}
	}
	return nil
}

func (s *Service) ListTimes(ctx context.Context, userID int64) ([]MealTimeDTO, error) {
	rows, err := s.store.ListMealTimes(ctx, sql.NullInt64{Int64: userID, Valid: true})
	if err != nil {
		return nil, fmt.Errorf("list meal times: %w", err)
	}
	times := make([]MealTimeDTO, 0, len(rows))
	for _, row := range rows {
		mt := MealTimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
		if row.UserID.Valid {
			id := row.UserID.Int64
			mt.UserID = &id
		}
		times = append(times, mt)
	}
	return times, nil
}

func (s *Service) CreateTime(ctx context.Context, userID int64, in CreateMealTimeInput) (MealTimeDTO, error) {
	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" {
		return MealTimeDTO{}, ValidationError{"name is required"}
	}
	start, err := parseTimeFlexible(in.StartTime)
	if err != nil {
		return MealTimeDTO{}, ValidationError{"invalid start_time format (expected HH:MM)"}
	}
	end, err := parseTimeFlexible(in.EndTime)
	if err != nil {
		return MealTimeDTO{}, ValidationError{"invalid end_time format (expected HH:MM)"}
	}
	row, err := s.store.CreateMealTime(ctx, CreateMealTimeParams{UserID: sql.NullInt64{Int64: userID, Valid: true}, Name: in.Name, Column3: start, Column4: end})
	if err != nil {
		if strings.Contains(err.Error(), "meal_times_user_name_idx") {
			return MealTimeDTO{}, ErrDuplicateMealTime
		}
		return MealTimeDTO{}, fmt.Errorf("create meal time: %w", err)
	}
	mt := MealTimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
	if row.UserID.Valid {
		id := row.UserID.Int64
		mt.UserID = &id
	}
	return mt, nil
}

func (s *Service) DeleteTime(ctx context.Context, userID, id int64) error {
	n, err := s.store.DeleteMealTime(ctx, DeleteMealTimeParams{ID: id, UserID: sql.NullInt64{Int64: userID, Valid: true}})
	if err != nil {
		return fmt.Errorf("delete meal time: %w", err)
	}
	if n == 0 {
		return ErrMealTimeNotFound
	}
	return nil
}

func (s *Service) ListPlans(ctx context.Context, userID int64, date, startDate, endDate string) ([]MealPlanDTO, error) {
	date, startDate, endDate = strings.TrimSpace(date), strings.TrimSpace(startDate), strings.TrimSpace(endDate)
	items := make([]MealPlanDTO, 0)
	switch {
	case date != "":
		if _, err := time.Parse("2006-01-02", date); err != nil {
			return nil, ValidationError{"invalid date format, expected YYYY-MM-DD"}
		}
		rows, err := s.store.ListMealPlansByDate(ctx, ListMealPlansByDateParams{UserID: userID, Column2: date})
		if err != nil {
			return nil, fmt.Errorf("list meal plans by date: %w", err)
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	case startDate != "" && endDate != "":
		if _, err := time.Parse("2006-01-02", startDate); err != nil {
			return nil, ValidationError{"invalid start_date format, expected YYYY-MM-DD"}
		}
		if _, err := time.Parse("2006-01-02", endDate); err != nil {
			return nil, ValidationError{"invalid end_date format, expected YYYY-MM-DD"}
		}
		rows, err := s.store.ListMealPlansByRange(ctx, ListMealPlansByRangeParams{UserID: userID, Column2: startDate, Column3: endDate})
		if err != nil {
			return nil, fmt.Errorf("list meal plans by range: %w", err)
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	default:
		rows, err := s.store.ListRecentMealPlans(ctx, userID)
		if err != nil {
			return nil, fmt.Errorf("list recent meal plans: %w", err)
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	}
	return items, nil
}

func (s *Service) CreatePlan(ctx context.Context, userID int64, in CreateMealPlanInput) (MealPlanDTO, error) {
	in.Date, in.Name = strings.TrimSpace(in.Date), strings.TrimSpace(in.Name)
	if in.Date == "" {
		return MealPlanDTO{}, ValidationError{"date is required"}
	}
	if _, err := time.Parse("2006-01-02", in.Date); err != nil {
		return MealPlanDTO{}, ValidationError{"invalid date format, expected YYYY-MM-DD"}
	}
	if in.Name == "" {
		return MealPlanDTO{}, ValidationError{"meal name cannot be empty"}
	}
	var start, end string
	var mtID sql.NullInt64
	if in.MealTimeID != nil {
		mtID = sql.NullInt64{Int64: *in.MealTimeID, Valid: true}
		rangeRow, err := s.store.GetMealTimeRange(ctx, GetMealTimeRangeParams{ID: *in.MealTimeID, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			return MealPlanDTO{}, ErrMealTimeNotFound
		}
		if err != nil {
			return MealPlanDTO{}, fmt.Errorf("get meal time: %w", err)
		}
		start, end = rangeRow.StartTime, rangeRow.EndTime
	}
	consumed := in.IsConsumed != nil && *in.IsConsumed
	row, err := s.store.CreateMealPlan(ctx, CreateMealPlanParams{UserID: userID, Column2: in.Date, Name: in.Name, MealTimeID: mtID, Column5: start, Column6: end, IsConsumed: consumed})
	if err != nil {
		return MealPlanDTO{}, fmt.Errorf("create meal plan: %w", err)
	}
	return mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}

func (s *Service) DeletePlan(ctx context.Context, userID, id int64) error {
	n, err := s.store.DeleteMealPlan(ctx, DeleteMealPlanParams{ID: id, UserID: userID})
	if err != nil {
		return fmt.Errorf("delete meal plan: %w", err)
	}
	if n == 0 {
		return ErrMealNotFound
	}
	return nil
}

func (s *Service) SetConsumed(ctx context.Context, userID, id int64, consumed bool) (MealPlanDTO, error) {
	row, err := s.store.SetMealPlanConsumed(ctx, SetMealPlanConsumedParams{IsConsumed: consumed, ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return MealPlanDTO{}, ErrMealNotFound
	}
	if err != nil {
		return MealPlanDTO{}, fmt.Errorf("set meal consumed: %w", err)
	}
	return mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}

func (s *Service) UpdatePlan(ctx context.Context, userID, id int64, in UpdateMealPlanInput) (MealPlanDTO, error) {
	current, err := s.store.GetMealPlanForUpdate(ctx, GetMealPlanForUpdateParams{ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return MealPlanDTO{}, ErrMealNotFound
	}
	if err != nil {
		return MealPlanDTO{}, fmt.Errorf("get meal plan: %w", err)
	}
	name, date, mtID, consumed := current.Name, current.Date, current.MealTimeID, current.IsConsumed
	if in.Name != nil {
		name = strings.TrimSpace(*in.Name)
		if name == "" {
			return MealPlanDTO{}, ValidationError{"meal name cannot be empty"}
		}
	}
	if in.Date != nil {
		date = strings.TrimSpace(*in.Date)
		if _, err := time.Parse("2006-01-02", date); err != nil {
			return MealPlanDTO{}, ValidationError{"invalid date format, expected YYYY-MM-DD"}
		}
	}
	var start, end string
	if in.MealTimeID != nil {
		mtID = sql.NullInt64{Int64: *in.MealTimeID, Valid: true}
		rangeRow, err := s.store.GetMealTimeRange(ctx, GetMealTimeRangeParams{ID: *in.MealTimeID, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			return MealPlanDTO{}, ErrMealTimeNotFound
		}
		if err != nil {
			return MealPlanDTO{}, fmt.Errorf("get meal time: %w", err)
		}
		start, end = rangeRow.StartTime, rangeRow.EndTime
	} else if current.MealTimeID.Valid {
		rangeRow, err := s.store.GetMealTimeRange(ctx, GetMealTimeRangeParams{ID: current.MealTimeID.Int64, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if err == nil {
			start, end = rangeRow.StartTime, rangeRow.EndTime
		}
	}
	if in.IsConsumed != nil {
		consumed = *in.IsConsumed
	}
	row, err := s.store.UpdateMealPlan(ctx, UpdateMealPlanParams{Name: name, Column2: date, MealTimeID: mtID, Column4: start, Column5: end, IsConsumed: consumed, ID: id, UserID: userID})
	if err != nil {
		return MealPlanDTO{}, fmt.Errorf("update meal plan: %w", err)
	}
	return mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}
