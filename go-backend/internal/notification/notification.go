package notification

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type notificationDTO struct {
	ID         int64   `json:"id"`
	Source     string  `json:"source"`
	Title      string  `json:"title"`
	Body       *string `json:"body"`
	TargetPath *string `json:"target_path"`
	Priority   int     `json:"priority"`
	CreatedAt  string  `json:"created_at"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func (h *Handler) ListNotifications(w http.ResponseWriter, r *http.Request) {
	user, ok := h.notificationUser(w, r)
	if !ok {
		return
	}

	limit := 100
	if rawLimit := r.URL.Query().Get("limit"); rawLimit != "" {
		parsedLimit, err := strconv.Atoi(rawLimit)
		if err != nil || parsedLimit < 1 || parsedLimit > 100 {
			webutil.BadRequest(w, "limit must be between 1 and 100")
			return
		}
		limit = parsedLimit
	}

	rows, err := sqlc.New(h.DB).ListActiveNotifications(r.Context(), sqlc.ListActiveNotificationsParams{UserID: user.ID, Limit: int32(limit)})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	notifications := make([]notificationDTO, 0)
	for _, row := range rows {
		notification := notificationDTO{ID: row.ID, Source: row.Source, Title: row.Title, Priority: int(row.Priority), CreatedAt: row.CreatedAt.UTC().Format("2006-01-02T15:04:05Z")}
		if row.Body.Valid {
			notification.Body = &row.Body.String
		}
		if row.TargetPath.Valid {
			notification.TargetPath = &row.TargetPath.String
		}
		notifications = append(notifications, notification)
	}
	webutil.WriteJSON(w, http.StatusOK, notifications)
}

func (h *Handler) DismissNotification(w http.ResponseWriter, r *http.Request) {
	user, ok := h.notificationUser(w, r)
	if !ok {
		return
	}
	notificationID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	updated, err := sqlc.New(h.DB).DismissNotification(r.Context(), sqlc.DismissNotificationParams{ID: notificationID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if updated == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ClearNotifications(w http.ResponseWriter, r *http.Request) {
	user, ok := h.notificationUser(w, r)
	if !ok {
		return
	}
	if err := sqlc.New(h.DB).DismissAllNotifications(r.Context(), user.ID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
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
