package gym

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
	"github.com/DarkAbhi/life-backend/internal/timeutil"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

const gymReminderSource = "Gym reminder"

type gymVisitListItem struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
}

type exerciseSetInput struct {
	Reps   int      `json:"reps"`
	Weight *float64 `json:"weight"`
}

type createGymExerciseBody struct {
	Name string             `json:"name"`
	Sets []exerciseSetInput `json:"sets"`
}

type gymExerciseSetDTO struct {
	ID        int64    `json:"id"`
	SetNumber int      `json:"set_number"`
	Reps      int      `json:"reps"`
	Weight    *float64 `json:"weight"`
}

type gymExerciseDTO struct {
	ID   int64               `json:"id"`
	Name string              `json:"name"`
	Sets []gymExerciseSetDTO `json:"sets"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// GymVisitedToday checks if the gym has been visited today.
func (h *Handler) GymVisitedToday(w http.ResponseWriter, r *http.Request) {
	start, end := timeutil.DayBoundsIndia(time.Now().UTC())
	visitID, err := sqlc.New(h.DB).GetGymVisitToday(r.Context(), sqlc.GetGymVisitTodayParams{CreatedAt: start, CreatedAt_2: end})
	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": false})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": true, "id": visitID})
}

// AddWorkoutForDay records a new gym visit.
func (h *Handler) AddWorkoutForDay(w http.ResponseWriter, r *http.Request) {
	visitID, err := sqlc.New(h.DB).CreateGymVisit(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"message": "success", "id": visitID})
}

// ListGymVisits returns every recorded gym visit, newest first.
func (h *Handler) ListGymVisits(w http.ResponseWriter, r *http.Request) {
	rows, err := sqlc.New(h.DB).ListGymVisits(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	visits := make([]gymVisitListItem, 0)
	for _, row := range rows {
		visit := gymVisitListItem{ID: row.ID, CreatedAt: row.CreatedAt}
		visit.CreatedAt = visit.CreatedAt.UTC()
		visits = append(visits, visit)
	}
	webutil.WriteJSON(w, http.StatusOK, visits)
}

// DeleteGymVisit removes a visit and its exercises and sets through database cascades.
func (h *Handler) DeleteGymVisit(w http.ResponseWriter, r *http.Request) {
	visitID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	deleted, err := sqlc.New(h.DB).DeleteGymVisit(r.Context(), visitID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if deleted == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GetGymVisitExercises gets exercises for a gym visit.
func (h *Handler) GetGymVisitExercises(w http.ResponseWriter, r *http.Request) {
	visitID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if !h.gymVisitExists(w, r, visitID) {
		return
	}

	rows, err := sqlc.New(h.DB).ListGymVisitExercises(r.Context(), visitID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	exercises := make([]gymExerciseDTO, 0)
	for _, row := range rows {
		exercise := gymExerciseDTO{ID: row.ID, Name: row.Name}
		sets, err := h.exerciseSets(exercise.ID)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		exercise.Sets = sets
		exercises = append(exercises, exercise)
	}
	webutil.WriteJSON(w, http.StatusOK, exercises)
}

// CreateGymVisitExercise creates an exercise set log for a visit.
func (h *Handler) CreateGymVisitExercise(w http.ResponseWriter, r *http.Request) {
	visitID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if !h.gymVisitExists(w, r, visitID) {
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body createGymExerciseBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	name := strings.TrimSpace(body.Name)
	if name == "" || len([]rune(name)) > 100 {
		webutil.BadRequest(w, "exercise name must be between 1 and 100 characters")
		return
	}
	if len(body.Sets) == 0 || len(body.Sets) > 20 {
		webutil.BadRequest(w, "provide between 1 and 20 sets")
		return
	}
	for _, set := range body.Sets {
		if set.Reps <= 0 || set.Reps > 1000 || (set.Weight != nil && *set.Weight < 0) {
			webutil.BadRequest(w, "each set needs positive reps and a non-negative weight")
			return
		}
	}

	tx, err := h.DB.BeginTx(r.Context(), nil)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback()
	q := sqlc.New(tx)

	created, err := q.CreateGymVisitExercise(r.Context(), sqlc.CreateGymVisitExerciseParams{GymVisitID: visitID, Name: name})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	exercise := gymExerciseDTO{ID: created.ID, Name: created.Name}

	exercise.Sets = make([]gymExerciseSetDTO, 0, len(body.Sets))
	for index, set := range body.Sets {
		row, err := q.CreateGymExerciseSet(r.Context(), sqlc.CreateGymExerciseSetParams{GymVisitExerciseID: exercise.ID, SetNumber: int16(index + 1), Reps: int16(set.Reps), Weight: set.Weight})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		savedSet := gymExerciseSetDTO{ID: row.ID, SetNumber: int(row.SetNumber), Reps: int(row.Reps), Weight: row.Weight}
		exercise.Sets = append(exercise.Sets, savedSet)
	}
	if err := tx.Commit(); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, exercise)
}

// MarkGymReminderVisited records a gym visit from the reminder card and dismisses it.
func (h *Handler) MarkGymReminderVisited(w http.ResponseWriter, r *http.Request) {
	user, ok := h.notificationUser(w, r)
	if !ok {
		return
	}
	notificationID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	tx, err := h.DB.Begin()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback()
	q := sqlc.New(tx)

	reminderExists, err := q.GymReminderExists(r.Context(), sqlc.GymReminderExistsParams{ID: notificationID, UserID: user.ID, Source: gymReminderSource})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !reminderExists {
		http.NotFound(w, r)
		return
	}

	visitID, err := q.CreateGymVisit(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if err := q.DismissGymReminder(r.Context(), notificationID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if err := tx.Commit(); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"id": visitID})
}

func (h *Handler) gymVisitExists(w http.ResponseWriter, r *http.Request, visitID int64) bool {
	_, err := sqlc.New(h.DB).GymVisitExists(r.Context(), visitID)
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return false
	}
	return true
}

func (h *Handler) exerciseSets(exerciseID int64) ([]gymExerciseSetDTO, error) {
	rows, err := sqlc.New(h.DB).ListGymExerciseSets(context.Background(), exerciseID)
	if err != nil {
		return nil, err
	}

	sets := make([]gymExerciseSetDTO, 0)
	for _, row := range rows {
		set := gymExerciseSetDTO{ID: row.ID, SetNumber: int(row.SetNumber), Reps: int(row.Reps), Weight: row.Weight}
		sets = append(sets, set)
	}
	return sets, nil
}

func (h *Handler) notificationUser(w http.ResponseWriter, r *http.Request) (auth.SessionUser, bool) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return auth.SessionUser{}, false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return auth.SessionUser{}, false
	}
	return user, true
}
