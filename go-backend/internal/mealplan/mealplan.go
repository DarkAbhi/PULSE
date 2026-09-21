package mealplan

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
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
	for _, mt := range defaultMealTimes {
		_, err := db.Exec(`
			INSERT INTO meal_times (name, start_time, end_time, is_default)
			VALUES ($1, $2::time, $3::time, true)
			ON CONFLICT (COALESCE(user_id, 0), LOWER(name)) DO NOTHING
		`, mt.Name, mt.StartTime, mt.EndTime)
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

	rows, err := h.DB.Query(`
		SELECT id, name, TO_CHAR(start_time, 'HH24:MI'), TO_CHAR(end_time, 'HH24:MI'), is_default, user_id
		FROM meal_times
		WHERE user_id IS NULL OR user_id = $1
		ORDER BY start_time ASC, id ASC
	`, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer rows.Close()

	times := make([]MealTimeDTO, 0)
	for rows.Next() {
		var mt MealTimeDTO
		var uid sql.NullInt64
		if err := rows.Scan(&mt.ID, &mt.Name, &mt.StartTime, &mt.EndTime, &mt.IsDefault, &uid); err != nil {
			webutil.ServerError(w, err)
			return
		}
		if uid.Valid {
			id := uid.Int64
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

	var mt MealTimeDTO
	var uid sql.NullInt64
	err = h.DB.QueryRow(`
		INSERT INTO meal_times (user_id, name, start_time, end_time, is_default)
		VALUES ($1, $2, $3::time, $4::time, false)
		RETURNING id, name, TO_CHAR(start_time, 'HH24:MI'), TO_CHAR(end_time, 'HH24:MI'), is_default, user_id
	`, user.ID, input.Name, startTime, endTime).Scan(&mt.ID, &mt.Name, &mt.StartTime, &mt.EndTime, &mt.IsDefault, &uid)
	if err != nil {
		if strings.Contains(err.Error(), "meal_times_user_name_idx") {
			webutil.BadRequest(w, "a meal time with this name already exists")
			return
		}
		webutil.ServerError(w, err)
		return
	}

	if uid.Valid {
		id := uid.Int64
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

	res, err := h.DB.Exec(`DELETE FROM meal_times WHERE id = $1 AND user_id = $2 AND is_default = false`, id, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	rows, _ := res.RowsAffected()
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

	baseQuery := `
		SELECT mp.id, TO_CHAR(mp.date, 'YYYY-MM-DD'), mp.name, mp.meal_time_id, mt.name,
		       TO_CHAR(COALESCE(mp.start_time, mt.start_time), 'HH24:MI'),
		       TO_CHAR(COALESCE(mp.end_time, mt.end_time), 'HH24:MI'),
		       mp.is_consumed,
		       mp.created_at
		FROM meal_plans mp
		LEFT JOIN meal_times mt ON mp.meal_time_id = mt.id
	`

	var rows *sql.Rows

	switch {
	case date != "":
		if _, err := time.Parse("2006-01-02", date); err != nil {
			webutil.BadRequest(w, "invalid date format, expected YYYY-MM-DD")
			return
		}
		rows, err = h.DB.Query(baseQuery+`
			WHERE mp.user_id = $1 AND mp.date = $2::date
			ORDER BY COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC
		`, user.ID, date)

	case startDate != "" && endDate != "":
		if _, err := time.Parse("2006-01-02", startDate); err != nil {
			webutil.BadRequest(w, "invalid start_date format, expected YYYY-MM-DD")
			return
		}
		if _, err := time.Parse("2006-01-02", endDate); err != nil {
			webutil.BadRequest(w, "invalid end_date format, expected YYYY-MM-DD")
			return
		}
		rows, err = h.DB.Query(baseQuery+`
			WHERE mp.user_id = $1 AND mp.date >= $2::date AND mp.date <= $3::date
			ORDER BY mp.date ASC, COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC
		`, user.ID, startDate, endDate)

	default:
		rows, err = h.DB.Query(baseQuery+`
			WHERE mp.user_id = $1
			ORDER BY mp.date DESC, COALESCE(mp.start_time, mt.start_time) ASC NULLS LAST, mp.created_at ASC, mp.id ASC
			LIMIT 100
		`, user.ID)
	}

	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer rows.Close()

	items := make([]MealPlanDTO, 0)
	for rows.Next() {
		var item MealPlanDTO
		var mtID sql.NullInt64
		var mtName, startTime, endTime sql.NullString
		if err := rows.Scan(&item.ID, &item.Date, &item.Name, &mtID, &mtName, &startTime, &endTime, &item.IsConsumed, &item.CreatedAt); err != nil {
			webutil.ServerError(w, err)
			return
		}
		if mtID.Valid {
			id := mtID.Int64
			item.MealTimeID = &id
		}
		if mtName.Valid {
			item.MealTimeName = &mtName.String
		}
		if startTime.Valid {
			item.StartTime = &startTime.String
		}
		if endTime.Valid {
			item.EndTime = &endTime.String
		}
		items = append(items, item)
	}

	webutil.WriteJSON(w, http.StatusOK, items)
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

	var startTime, endTime sql.NullString
	if input.MealTimeID != nil {
		// Look up start_time and end_time from meal_times
		err := h.DB.QueryRow(`
			SELECT TO_CHAR(start_time, 'HH24:MI:SS'), TO_CHAR(end_time, 'HH24:MI:SS')
			FROM meal_times
			WHERE id = $1 AND (user_id IS NULL OR user_id = $2)
		`, *input.MealTimeID, user.ID).Scan(&startTime, &endTime)
		if errors.Is(err, sql.ErrNoRows) {
			webutil.BadRequest(w, "specified meal_time_id not found")
			return
		}
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
	}

	isConsumed := false
	if input.IsConsumed != nil {
		isConsumed = *input.IsConsumed
	}

	var item MealPlanDTO
	var mtID sql.NullInt64
	var mtName, resStartTime, resEndTime sql.NullString

	err = h.DB.QueryRow(`
		WITH inserted AS (
			INSERT INTO meal_plans (user_id, date, name, meal_time_id, start_time, end_time, is_consumed)
			VALUES ($1, $2::date, $3, $4, $5::time, $6::time, $7)
			RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
		)
		SELECT i.id, TO_CHAR(i.date, 'YYYY-MM-DD'), i.name, i.meal_time_id, mt.name,
		       TO_CHAR(COALESCE(i.start_time, mt.start_time), 'HH24:MI'),
		       TO_CHAR(COALESCE(i.end_time, mt.end_time), 'HH24:MI'),
		       i.is_consumed,
		       i.created_at
		FROM inserted i
		LEFT JOIN meal_times mt ON i.meal_time_id = mt.id
	`, user.ID, input.Date, input.Name, input.MealTimeID, startTime, endTime, isConsumed).
		Scan(&item.ID, &item.Date, &item.Name, &mtID, &mtName, &resStartTime, &resEndTime, &item.IsConsumed, &item.CreatedAt)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if mtID.Valid {
		id := mtID.Int64
		item.MealTimeID = &id
	}
	if mtName.Valid {
		item.MealTimeName = &mtName.String
	}
	if resStartTime.Valid {
		item.StartTime = &resStartTime.String
	}
	if resEndTime.Valid {
		item.EndTime = &resEndTime.String
	}

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

	res, err := h.DB.Exec(`DELETE FROM meal_plans WHERE id = $1 AND user_id = $2`, id, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	rowsAffected, _ := res.RowsAffected()
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

	var item MealPlanDTO
	var mtID sql.NullInt64
	var mtName, resStartTime, resEndTime sql.NullString

	err = h.DB.QueryRow(`
		WITH updated AS (
			UPDATE meal_plans
			SET is_consumed = $1, updated_at = CURRENT_TIMESTAMP
			WHERE id = $2 AND user_id = $3
			RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
		)
		SELECT u.id, TO_CHAR(u.date, 'YYYY-MM-DD'), u.name, u.meal_time_id, mt.name,
		       TO_CHAR(COALESCE(u.start_time, mt.start_time), 'HH24:MI'),
		       TO_CHAR(COALESCE(u.end_time, mt.end_time), 'HH24:MI'),
		       u.is_consumed,
		       u.created_at
		FROM updated u
		LEFT JOIN meal_times mt ON u.meal_time_id = mt.id
	`, input.IsConsumed, id, user.ID).Scan(&item.ID, &item.Date, &item.Name, &mtID, &mtName, &resStartTime, &resEndTime, &item.IsConsumed, &item.CreatedAt)

	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if mtID.Valid {
		idVal := mtID.Int64
		item.MealTimeID = &idVal
	}
	if mtName.Valid {
		item.MealTimeName = &mtName.String
	}
	if resStartTime.Valid {
		item.StartTime = &resStartTime.String
	}
	if resEndTime.Valid {
		item.EndTime = &resEndTime.String
	}

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
	var currentName, currentDate string
	var currentMtID sql.NullInt64
	var currentIsConsumed bool
	err = h.DB.QueryRow(`
		SELECT name, TO_CHAR(date, 'YYYY-MM-DD'), meal_time_id, is_consumed
		FROM meal_plans
		WHERE id = $1 AND user_id = $2
	`, id, user.ID).Scan(&currentName, &currentDate, &currentMtID, &currentIsConsumed)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	newName := currentName
	if input.Name != nil {
		trimmed := strings.TrimSpace(*input.Name)
		if trimmed == "" {
			webutil.BadRequest(w, "meal name cannot be empty")
			return
		}
		newName = trimmed
	}

	newDate := currentDate
	if input.Date != nil {
		trimmed := strings.TrimSpace(*input.Date)
		if _, err := time.Parse("2006-01-02", trimmed); err != nil {
			webutil.BadRequest(w, "invalid date format, expected YYYY-MM-DD")
			return
		}
		newDate = trimmed
	}

	newMtID := currentMtID
	var startTime, endTime sql.NullString
	if input.MealTimeID != nil {
		newMtID = sql.NullInt64{Int64: *input.MealTimeID, Valid: true}
		err := h.DB.QueryRow(`
			SELECT TO_CHAR(start_time, 'HH24:MI:SS'), TO_CHAR(end_time, 'HH24:MI:SS')
			FROM meal_times
			WHERE id = $1 AND (user_id IS NULL OR user_id = $2)
		`, *input.MealTimeID, user.ID).Scan(&startTime, &endTime)
		if errors.Is(err, sql.ErrNoRows) {
			webutil.BadRequest(w, "specified meal_time_id not found")
			return
		}
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
	} else if currentMtID.Valid {
		_ = h.DB.QueryRow(`
			SELECT TO_CHAR(start_time, 'HH24:MI:SS'), TO_CHAR(end_time, 'HH24:MI:SS')
			FROM meal_times
			WHERE id = $1 AND (user_id IS NULL OR user_id = $2)
		`, currentMtID.Int64, user.ID).Scan(&startTime, &endTime)
	}

	newIsConsumed := currentIsConsumed
	if input.IsConsumed != nil {
		newIsConsumed = *input.IsConsumed
	}

	var item MealPlanDTO
	var resMtID sql.NullInt64
	var mtName, resStartTime, resEndTime sql.NullString

	err = h.DB.QueryRow(`
		WITH updated AS (
			UPDATE meal_plans
			SET name = $1, date = $2::date, meal_time_id = $3, start_time = $4::time, end_time = $5::time, is_consumed = $6, updated_at = CURRENT_TIMESTAMP
			WHERE id = $7 AND user_id = $8
			RETURNING id, date, name, meal_time_id, start_time, end_time, is_consumed, created_at
		)
		SELECT u.id, TO_CHAR(u.date, 'YYYY-MM-DD'), u.name, u.meal_time_id, mt.name,
		       TO_CHAR(COALESCE(u.start_time, mt.start_time), 'HH24:MI'),
		       TO_CHAR(COALESCE(u.end_time, mt.end_time), 'HH24:MI'),
		       u.is_consumed,
		       u.created_at
		FROM updated u
		LEFT JOIN meal_times mt ON u.meal_time_id = mt.id
	`, newName, newDate, newMtID, startTime, endTime, newIsConsumed, id, user.ID).
		Scan(&item.ID, &item.Date, &item.Name, &resMtID, &mtName, &resStartTime, &resEndTime, &item.IsConsumed, &item.CreatedAt)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if resMtID.Valid {
		idVal := resMtID.Int64
		item.MealTimeID = &idVal
	}
	if mtName.Valid {
		item.MealTimeName = &mtName.String
	}
	if resStartTime.Valid {
		item.StartTime = &resStartTime.String
	}
	if resEndTime.Valid {
		item.EndTime = &resEndTime.String
	}

	webutil.WriteJSON(w, http.StatusOK, item)
}
