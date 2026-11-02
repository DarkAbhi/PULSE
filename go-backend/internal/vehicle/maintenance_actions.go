package vehicle

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"regexp"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type maintenanceAction struct {
	Component   string `json:"component"`
	Action      string `json:"action"`
	Description string `json:"description"`
}

var componentPattern = regexp.MustCompile(`^[a-z][a-z0-9_]{0,63}$`)
var maintenanceActionKinds = map[string]bool{
	"replaced": true, "inspected": true, "adjusted": true, "repaired": true,
	"topped_up": true, "cleaned": true, "lubricated": true, "completed": true,
}

func normalizeMaintenanceActions(actions *[]maintenanceAction) error {
	if actions == nil {
		return nil
	}
	if len(*actions) > 100 {
		return errors.New("a service can contain at most 100 actions")
	}
	seen := map[string]bool{}
	for i := range *actions {
		a := &(*actions)[i]
		a.Component = strings.ToLower(strings.TrimSpace(a.Component))
		a.Action = strings.ToLower(strings.TrimSpace(a.Action))
		a.Description = strings.TrimSpace(a.Description)
		if !componentPattern.MatchString(a.Component) || !maintenanceActionKinds[a.Action] || len(a.Description) > 500 {
			return errors.New("each action needs a component key, a supported action, and a description of at most 500 bytes")
		}
		key := a.Component + ":" + a.Action
		if seen[key] {
			return errors.New("duplicate component and action in service")
		}
		seen[key] = true
	}
	return nil
}

func saveMaintenanceActions(ctx context.Context, q *query.Queries, recordID, userID int64, actions *[]maintenanceAction) error {
	// Omitted/null actions preserve existing work for clients predating this feature.
	if actions == nil {
		return nil
	}
	if err := q.DeleteMaintenanceActions(ctx, query.DeleteMaintenanceActionsParams{MaintenanceRecordID: recordID, UserID: userID}); err != nil {
		return fmt.Errorf("replace maintenance actions: %w", err)
	}
	for _, a := range *actions {
		if err := q.CreateMaintenanceAction(ctx, query.CreateMaintenanceActionParams{
			MaintenanceRecordID: recordID, UserID: userID, Component: a.Component, Action: a.Action, Description: a.Description,
		}); err != nil {
			return fmt.Errorf("save maintenance action: %w", err)
		}
	}
	return nil
}

func populateMaintenanceRecord(ctx context.Context, q *query.Queries, userID int64, record *maintenanceRecord) error {
	actions, err := q.ListMaintenanceActions(ctx, query.ListMaintenanceActionsParams{MaintenanceRecordID: record.ID, UserID: userID})
	if err != nil {
		return fmt.Errorf("list maintenance actions: %w", err)
	}
	record.Actions = []maintenanceAction{}
	for _, a := range actions {
		record.Actions = append(record.Actions, maintenanceAction{a.Component, a.Action, a.Description})
	}
	attachments, err := q.ListMaintenanceAttachments(ctx, query.ListMaintenanceAttachmentsParams{MaintenanceRecordID: record.ID, UserID: userID})
	if err != nil {
		return fmt.Errorf("list maintenance attachments: %w", err)
	}
	record.Attachments = []maintenanceAttachment{}
	for _, a := range attachments {
		record.Attachments = append(record.Attachments, maintenanceAttachment{ID: a.ID, FileName: a.FileName, ContentType: a.ContentType, SizeBytes: a.SizeBytes, CreatedAt: a.CreatedAt.Time.UTC()})
	}
	return nil
}

func (h *Handler) MaintenanceHistory(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	component, action := r.URL.Query().Get("component"), r.URL.Query().Get("action")
	if !componentPattern.MatchString(component) || (action != "" && !maintenanceActionKinds[action]) {
		webutil.BadRequest(w, "provide a component key and an optional supported action")
		return
	}
	q := query.New(h.DB)
	rows, err := q.SearchMaintenanceHistory(r.Context(), query.SearchMaintenanceHistoryParams{VehicleID: vehicleID, UserID: user.ID, Component: component, Action: action})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	records := []maintenanceRecord{}
	for _, row := range rows {
		record := maintenanceRecordDTO(row.ID, row.Category, row.Title, row.Amount, row.OccurredAt.Time, row.OdometerKm, row.ProviderName, textToNullString(row.Notes))
		if err := populateMaintenanceRecord(r.Context(), q, user.ID, &record); err != nil {
			webutil.ServerError(w, err)
			return
		}
		records = append(records, record)
	}
	webutil.WriteJSON(w, http.StatusOK, records)
}
