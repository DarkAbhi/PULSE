package mealplan

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type DefaultMealTime struct {
	Name      string
	StartTime string
	EndTime   string
}

var defaultMealTimes = []DefaultMealTime{
	{Name: "Breakfast", StartTime: "07:00:00", EndTime: "10:00:00"},
	{Name: "Lunch", StartTime: "12:00:00", EndTime: "14:30:00"},
	{Name: "Evening Snacks", StartTime: "16:30:00", EndTime: "18:30:00"},
	{Name: "Dinner", StartTime: "19:30:00", EndTime: "22:00:00"},
}

// EnsureDefaultMealTimes guarantees default meal times exist in the database.
func EnsureDefaultMealTimes(db *sql.DB) error {
	q := sqlc.New(db)
	for _, mt := range defaultMealTimes {
		err := q.EnsureDefaultMealTime(context.Background(), sqlc.EnsureDefaultMealTimeParams{Name: mt.Name, Column2: mt.StartTime, Column3: mt.EndTime})
		if err != nil {
			return err
		}
	}
	return nil
}

type MealTimeDTO struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	StartTime string `json:"start_time"` // "HH:MM"
	EndTime   string `json:"end_time"`   // "HH:MM"
	IsDefault bool   `json:"is_default"`
	UserID    *int64 `json:"user_id,omitempty"`
}

type CreateMealTimeInput struct {
	Name      string `json:"name"`
	StartTime string `json:"start_time"` // accepts "HH:MM" or "HH:MM:SS"
	EndTime   string `json:"end_time"`   // accepts "HH:MM" or "HH:MM:SS"
}

type MealPlanDTO struct {
	ID           int64     `json:"id"`
	Date         string    `json:"date"`
	Name         string    `json:"name"`
	MealTimeID   *int64    `json:"meal_time_id,omitempty"`
	MealTimeName *string   `json:"meal_time_name,omitempty"`
	StartTime    *string   `json:"start_time,omitempty"`
	EndTime      *string   `json:"end_time,omitempty"`
	IsConsumed   bool      `json:"is_consumed"`
	CreatedAt    time.Time `json:"created_at"`
}

type CreateMealPlanInput struct {
	Date       string `json:"date"`
	Name       string `json:"name"`
	MealTimeID *int64 `json:"meal_time_id,omitempty"`
	IsConsumed *bool  `json:"is_consumed,omitempty"`
}

type UpdateMealPlanConsumedInput struct {
	IsConsumed bool `json:"is_consumed"`
}

type UpdateMealPlanInput struct {
	Date       *string `json:"date,omitempty"`
	Name       *string `json:"name,omitempty"`
	MealTimeID *int64  `json:"meal_time_id,omitempty"`
	IsConsumed *bool   `json:"is_consumed,omitempty"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// parseTimeFlexible accepts "HH:MM" or "HH:MM:SS"
func parseTimeFlexible(val string) (string, error) {
	val = strings.TrimSpace(val)
	if len(val) == 5 {
		if _, err := time.Parse("15:04", val); err != nil {
			return "", err
		}
		return val + ":00", nil
	}
	if len(val) == 8 {
		if _, err := time.Parse("15:04:05", val); err != nil {
			return "", err
		}
		return val, nil
	}
	return "", errors.New("invalid time format, expected HH:MM")
}

// ListMealTimes returns all default and custom meal times.
func (h *Handler) ListMealTimes(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	rows, err := sqlc.New(h.DB).ListMealTimes(r.Context(), sql.NullInt64{Int64: user.ID, Valid: true})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	times := make([]MealTimeDTO, 0)
	for _, row := range rows {
		mt := MealTimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
		if row.UserID.Valid {
			id := row.UserID.Int64
			mt.UserID = &id
		}
		times = append(times, mt)
	}

	webutil.WriteJSON(w, http.StatusOK, times)
}

// CreateMealTime creates a user-defined meal time.
func (h *Handler) CreateMealTime(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var input CreateMealTimeInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}

	input.Name = strings.TrimSpace(input.Name)
	if input.Name == "" {
		webutil.BadRequest(w, "name is required")
		return
	}

	startTime, err := parseTimeFlexible(input.StartTime)
	if err != nil {
		webutil.BadRequest(w, "invalid start_time format (expected HH:MM)")
		return
	}

	endTime, err := parseTimeFlexible(input.EndTime)
	if err != nil {
		webutil.BadRequest(w, "invalid end_time format (expected HH:MM)")
		return
	}

	row, err := sqlc.New(h.DB).CreateMealTime(r.Context(), sqlc.CreateMealTimeParams{UserID: sql.NullInt64{Int64: user.ID, Valid: true}, Name: input.Name, Column3: startTime, Column4: endTime})
	if err != nil {
		if strings.Contains(err.Error(), "meal_times_user_name_idx") {
			webutil.BadRequest(w, "a meal time with this name already exists")
			return
		}
		webutil.ServerError(w, err)
		return
	}

	mt := MealTimeDTO{ID: row.ID, Name: row.Name, StartTime: row.StartTime, EndTime: row.EndTime, IsDefault: row.IsDefault}
	if row.UserID.Valid {
		id := row.UserID.Int64
		mt.UserID = &id
	}

	webutil.WriteJSON(w, http.StatusCreated, mt)
}

// DeleteMealTime deletes a user-defined meal time.
func (h *Handler) DeleteMealTime(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	rows, err := sqlc.New(h.DB).DeleteMealTime(r.Context(), sqlc.DeleteMealTimeParams{ID: id, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if rows == 0 {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "custom meal time not found or cannot be deleted"})
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// ListMealPlans returns meals for the authenticated user with meal time details.
func (h *Handler) ListMealPlans(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	date := strings.TrimSpace(r.URL.Query().Get("date"))
	startDate := strings.TrimSpace(r.URL.Query().Get("start_date"))
	endDate := strings.TrimSpace(r.URL.Query().Get("end_date"))

	q := sqlc.New(h.DB)
	items := make([]MealPlanDTO, 0)
	switch {
	case date != "":
		if _, err := time.Parse("2006-01-02", date); err != nil {
			webutil.BadRequest(w, "invalid date format, expected YYYY-MM-DD")
			return
		}
		rows, err := q.ListMealPlansByDate(r.Context(), sqlc.ListMealPlansByDateParams{UserID: user.ID, Column2: date})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}

	case startDate != "" && endDate != "":
		if _, err := time.Parse("2006-01-02", startDate); err != nil {
			webutil.BadRequest(w, "invalid start_date format, expected YYYY-MM-DD")
			return
		}
		if _, err := time.Parse("2006-01-02", endDate); err != nil {
			webutil.BadRequest(w, "invalid end_date format, expected YYYY-MM-DD")
			return
		}
		rows, err := q.ListMealPlansByRange(r.Context(), sqlc.ListMealPlansByRangeParams{UserID: user.ID, Column2: startDate, Column3: endDate})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}

	default:
		rows, err := q.ListRecentMealPlans(r.Context(), user.ID)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		for _, row := range rows {
			items = append(items, mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt))
		}
	}

	webutil.WriteJSON(w, http.StatusOK, items)
}

func mealPlanDTO(id int64, date, name string, mealTimeID sql.NullInt64, mealTimeName sql.NullString, start, end string, consumed bool, created time.Time) MealPlanDTO {
	item := MealPlanDTO{ID: id, Date: date, Name: name, IsConsumed: consumed, CreatedAt: created}
	if mealTimeID.Valid {
		item.MealTimeID = &mealTimeID.Int64
	}
	if mealTimeName.Valid {
		item.MealTimeName = &mealTimeName.String
	}
	if start != "" {
		item.StartTime = &start
	}
	if end != "" {
		item.EndTime = &end
	}
	return item
}

// CreateMealPlan creates a new meal for a given date, optionally with meal_time_id.
func (h *Handler) CreateMealPlan(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var input CreateMealPlanInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}

	input.Date = strings.TrimSpace(input.Date)
	input.Name = strings.TrimSpace(input.Name)

	if input.Date == "" {
		webutil.BadRequest(w, "date is required")
		return
	}
	if _, err := time.Parse("2006-01-02", input.Date); err != nil {
		webutil.BadRequest(w, "invalid date format, expected YYYY-MM-DD")
		return
	}
	if input.Name == "" {
		webutil.BadRequest(w, "meal name cannot be empty")
		return
	}

	var startTime, endTime string
	if input.MealTimeID != nil {
		rangeRow, err := sqlc.New(h.DB).GetMealTimeRange(r.Context(), sqlc.GetMealTimeRangeParams{ID: *input.MealTimeID, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			webutil.BadRequest(w, "specified meal_time_id not found")
			return
		}
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		startTime, endTime = rangeRow.StartTime, rangeRow.EndTime
	}

	isConsumed := false
	if input.IsConsumed != nil {
		isConsumed = *input.IsConsumed
	}

	var mtID sql.NullInt64
	if input.MealTimeID != nil {
		mtID = sql.NullInt64{Int64: *input.MealTimeID, Valid: true}
	}
	row, err := sqlc.New(h.DB).CreateMealPlan(r.Context(), sqlc.CreateMealPlanParams{UserID: user.ID, Column2: input.Date, Name: input.Name, MealTimeID: mtID, Column5: startTime, Column6: endTime, IsConsumed: isConsumed})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt)

	webutil.WriteJSON(w, http.StatusCreated, item)
}

// DeleteMealPlan deletes a meal by ID for the authenticated user.
func (h *Handler) DeleteMealPlan(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	rowsAffected, err := sqlc.New(h.DB).DeleteMealPlan(r.Context(), sqlc.DeleteMealPlanParams{ID: id, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if rowsAffected == 0 {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// UpdateMealPlanConsumed updates the is_consumed checklist status for a meal.
func (h *Handler) UpdateMealPlanConsumed(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var input UpdateMealPlanConsumedInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}

	row, err := sqlc.New(h.DB).SetMealPlanConsumed(r.Context(), sqlc.SetMealPlanConsumedParams{IsConsumed: input.IsConsumed, ID: id, UserID: user.ID})

	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt)

	webutil.WriteJSON(w, http.StatusOK, item)
}

// UpdateMealPlan allows updating meal plan fields (name, date, meal_time_id, is_consumed).
func (h *Handler) UpdateMealPlan(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var input UpdateMealPlanInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}

	// Fetch current meal plan
	q := sqlc.New(h.DB)
	current, err := q.GetMealPlanForUpdate(r.Context(), sqlc.GetMealPlanForUpdateParams{ID: id, UserID: user.ID})
	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	newName := current.Name
	if input.Name != nil {
		trimmed := strings.TrimSpace(*input.Name)
		if trimmed == "" {
			webutil.BadRequest(w, "meal name cannot be empty")
			return
		}
		newName = trimmed
	}

	newDate := current.Date
	if input.Date != nil {
		trimmed := strings.TrimSpace(*input.Date)
		if _, err := time.Parse("2006-01-02", trimmed); err != nil {
			webutil.BadRequest(w, "invalid date format, expected YYYY-MM-DD")
			return
		}
		newDate = trimmed
	}

	newMtID := current.MealTimeID
	var startTime, endTime string
	if input.MealTimeID != nil {
		newMtID = sql.NullInt64{Int64: *input.MealTimeID, Valid: true}
		rangeRow, err := q.GetMealTimeRange(r.Context(), sqlc.GetMealTimeRangeParams{ID: *input.MealTimeID, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
		if errors.Is(err, sql.ErrNoRows) {
			webutil.BadRequest(w, "specified meal_time_id not found")
			return
		}
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		startTime, endTime = rangeRow.StartTime, rangeRow.EndTime
	} else if current.MealTimeID.Valid {
		rangeRow, err := q.GetMealTimeRange(r.Context(), sqlc.GetMealTimeRangeParams{ID: current.MealTimeID.Int64, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
		if err == nil {
			startTime, endTime = rangeRow.StartTime, rangeRow.EndTime
		}
	}

	newIsConsumed := current.IsConsumed
	if input.IsConsumed != nil {
		newIsConsumed = *input.IsConsumed
	}

	row, err := q.UpdateMealPlan(r.Context(), sqlc.UpdateMealPlanParams{Name: newName, Column2: newDate, MealTimeID: newMtID, Column4: startTime, Column5: endTime, IsConsumed: newIsConsumed, ID: id, UserID: user.ID})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := mealPlanDTO(row.ID, row.Date, row.Name, row.MealTimeID, row.MealTimeName, row.StartTime, row.EndTime, row.IsConsumed, row.CreatedAt)

	webutil.WriteJSON(w, http.StatusOK, item)
}
