package mealplan

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"
)

var ErrPlanNotFound = errors.New("mealplan: meal not found")
var ErrTimeNotFound = errors.New("mealplan: meal time not found")
var ErrDuplicateTime = errors.New("mealplan: meal time already exists")

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type store interface {
	EnsureDefaultTime(context.Context, EnsureDefaultTimeParams) error
	ListTimes(context.Context, sql.NullInt64) ([]ListTimesRow, error)
	CreateTime(context.Context, CreateTimeParams) (CreateTimeRow, error)
	DeleteTime(context.Context, DeleteTimeParams) (int64, error)
	ListPlansByDate(context.Context, ListPlansByDateParams) ([]ListPlansByDateRow, error)
	ListPlansByRange(context.Context, ListPlansByRangeParams) ([]ListPlansByRangeRow, error)
	ListRecentPlans(context.Context, int64) ([]ListRecentPlansRow, error)
	FetchTimeRange(context.Context, TimeRangeParams) (TimeRangeRow, error)
	CreatePlan(context.Context, CreatePlanParams) (CreatePlanRow, error)
	DeletePlan(context.Context, DeletePlanParams) (int64, error)
	SetPlanConsumed(context.Context, SetPlanConsumedParams) (SetPlanConsumedRow, error)
	FetchPlanForUpdate(context.Context, PlanForUpdateParams) (PlanForUpdateRow, error)
	UpdatePlan(context.Context, UpdatePlanParams) (UpdatePlanRow, error)
}

type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) EnsureDefaults(ctx context.Context) error {
	for _, mt := range defaultTimes {
		if err := s.store.EnsureDefaultTime(ctx, EnsureDefaultTimeParams{Name: mt.Name, Column2: mt.StartTime, Column3: mt.EndTime}); err != nil {
			return fmt.Errorf("ensure default meal time: %w", err)
		}
	}
	return nil
}

func (s *Service) ListTimes(ctx context.Context, userID int64) ([]TimeDTO, error) {
	rows, err := s.store.ListTimes(ctx, sql.NullInt64{Int64: userID, Valid: true})
	if err != nil {
		return nil, fmt.Errorf("list meal times: %w", err)
	}
	times := make([]TimeDTO, 0, len(rows))
	for _, row := range rows {
		mt := TimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
		if row.UserID.Valid {
			id := row.UserID.Int64
			mt.UserID = &id
		}
		times = append(times, mt)
	}
	return times, nil
}

func (s *Service) CreateTime(ctx context.Context, userID int64, in CreateTimeInput) (TimeDTO, error) {
	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" {
		return TimeDTO{}, ValidationError{"name is required"}
	}
	start, err := parseTimeFlexible(in.StartTime)
	if err != nil {
		return TimeDTO{}, ValidationError{"invalid start time format (expected hh:mm)"}
	}
	end, err := parseTimeFlexible(in.EndTime)
	if err != nil {
		return TimeDTO{}, ValidationError{"invalid end time format (expected hh:mm)"}
	}
	row, err := s.store.CreateTime(ctx, CreateTimeParams{UserID: sql.NullInt64{Int64: userID, Valid: true}, Name: in.Name, Column3: start, Column4: end})
	if err != nil {
		if strings.Contains(err.Error(), "meal_times_user_name_idx") {
			return TimeDTO{}, ErrDuplicateTime
		}
		return TimeDTO{}, fmt.Errorf("create meal time: %w", err)
	}
	mt := TimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
	if row.UserID.Valid {
		id := row.UserID.Int64
		mt.UserID = &id
	}
	return mt, nil
}

func (s *Service) DeleteTime(ctx context.Context, userID, id int64) error {
	n, err := s.store.DeleteTime(ctx, DeleteTimeParams{ID: id, UserID: sql.NullInt64{Int64: userID, Valid: true}})
	if err != nil {
		return fmt.Errorf("delete meal time: %w", err)
	}
	if n == 0 {
		return ErrTimeNotFound
	}
	return nil
}

func (s *Service) ListPlans(ctx context.Context, userID int64, date, startDate, endDate string) ([]PlanDTO, error) {
	date, startDate, endDate = strings.TrimSpace(date), strings.TrimSpace(startDate), strings.TrimSpace(endDate)
	items := make([]PlanDTO, 0)
	switch {
	case date != "":
		if _, err := time.Parse("2006-01-02", date); err != nil {
			return nil, ValidationError{"invalid date format, expected yyyy-mm-dd"}
		}
		rows, err := s.store.ListPlansByDate(ctx, ListPlansByDateParams{UserID: userID, Column2: date})
		if err != nil {
			return nil, fmt.Errorf("list meal plans by date: %w", err)
		}
		for _, row := range rows {
			items = append(items, planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	case startDate != "" && endDate != "":
		if _, err := time.Parse("2006-01-02", startDate); err != nil {
			return nil, ValidationError{"invalid start date format, expected yyyy-mm-dd"}
		}
		if _, err := time.Parse("2006-01-02", endDate); err != nil {
			return nil, ValidationError{"invalid end date format, expected yyyy-mm-dd"}
		}
		rows, err := s.store.ListPlansByRange(ctx, ListPlansByRangeParams{UserID: userID, Column2: startDate, Column3: endDate})
		if err != nil {
			return nil, fmt.Errorf("list meal plans by range: %w", err)
		}
		for _, row := range rows {
			items = append(items, planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	default:
		rows, err := s.store.ListRecentPlans(ctx, userID)
		if err != nil {
			return nil, fmt.Errorf("list recent meal plans: %w", err)
		}
		for _, row := range rows {
			items = append(items, planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	}
	return items, nil
}

func (s *Service) CreatePlan(ctx context.Context, userID int64, in CreatePlanInput) (PlanDTO, error) {
	in.Date, in.Name = strings.TrimSpace(in.Date), strings.TrimSpace(in.Name)
	if in.Date == "" {
		return PlanDTO{}, ValidationError{"date is required"}
	}
	if _, err := time.Parse("2006-01-02", in.Date); err != nil {
		return PlanDTO{}, ValidationError{"invalid date format, expected yyyy-mm-dd"}
	}
	if in.Name == "" {
		return PlanDTO{}, ValidationError{"meal name cannot be empty"}
	}
	var start, end string
	var mtID sql.NullInt64
	if in.MealTimeID != nil {
		mtID = sql.NullInt64{Int64: *in.MealTimeID, Valid: true}
		rangeRow, err := s.store.FetchTimeRange(ctx, TimeRangeParams{ID: *in.MealTimeID, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			return PlanDTO{}, ErrTimeNotFound
		}
		if err != nil {
			return PlanDTO{}, fmt.Errorf("get meal time: %w", err)
		}
		start, end = rangeRow.StartTime, rangeRow.EndTime
	}
	isConsumed := in.IsConsumed != nil && *in.IsConsumed
	row, err := s.store.CreatePlan(ctx, CreatePlanParams{UserID: userID, Column2: in.Date, Name: in.Name, MealTimeID: mtID, Column5: start, Column6: end, IsConsumed: isConsumed})
	if err != nil {
		return PlanDTO{}, fmt.Errorf("create meal plan: %w", err)
	}
	return planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}

func (s *Service) DeletePlan(ctx context.Context, userID, id int64) error {
	n, err := s.store.DeletePlan(ctx, DeletePlanParams{ID: id, UserID: userID})
	if err != nil {
		return fmt.Errorf("delete meal plan: %w", err)
	}
	if n == 0 {
		return ErrPlanNotFound
	}
	return nil
}

func (s *Service) SetConsumed(ctx context.Context, userID, id int64, isConsumed bool) (PlanDTO, error) {
	row, err := s.store.SetPlanConsumed(ctx, SetPlanConsumedParams{IsConsumed: isConsumed, ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return PlanDTO{}, ErrPlanNotFound
	}
	if err != nil {
		return PlanDTO{}, fmt.Errorf("set meal consumed: %w", err)
	}
	return planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}

func (s *Service) UpdatePlan(ctx context.Context, userID, id int64, in UpdatePlanInput) (PlanDTO, error) {
	current, err := s.store.FetchPlanForUpdate(ctx, PlanForUpdateParams{ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return PlanDTO{}, ErrPlanNotFound
	}
	if err != nil {
		return PlanDTO{}, fmt.Errorf("get meal plan: %w", err)
	}
	name, date, mtID, isConsumed := current.Name, current.Date, current.MealTimeID, current.IsConsumed
	if in.Name != nil {
		name = strings.TrimSpace(*in.Name)
		if name == "" {
			return PlanDTO{}, ValidationError{"meal name cannot be empty"}
		}
	}
	if in.Date != nil {
		date = strings.TrimSpace(*in.Date)
		if _, err := time.Parse("2006-01-02", date); err != nil {
			return PlanDTO{}, ValidationError{"invalid date format, expected yyyy-mm-dd"}
		}
	}
	var start, end string
	if in.MealTimeID != nil {
		mtID = sql.NullInt64{Int64: *in.MealTimeID, Valid: true}
		rangeRow, err := s.store.FetchTimeRange(ctx, TimeRangeParams{ID: *in.MealTimeID, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			return PlanDTO{}, ErrTimeNotFound
		}
		if err != nil {
			return PlanDTO{}, fmt.Errorf("get meal time: %w", err)
		}
		start, end = rangeRow.StartTime, rangeRow.EndTime
	} else if current.MealTimeID.Valid {
		rangeRow, err := s.store.FetchTimeRange(ctx, TimeRangeParams{ID: current.MealTimeID.Int64, UserID: sql.NullInt64{Int64: userID, Valid: true}})
		if err == nil {
			start, end = rangeRow.StartTime, rangeRow.EndTime
		}
	}
	if in.IsConsumed != nil {
		isConsumed = *in.IsConsumed
	}
	row, err := s.store.UpdatePlan(ctx, UpdatePlanParams{Name: name, Column2: date, MealTimeID: mtID, Column4: start, Column5: end, IsConsumed: isConsumed, ID: id, UserID: userID})
	if err != nil {
		return PlanDTO{}, fmt.Errorf("update meal plan: %w", err)
	}
	return planDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt), nil
}
