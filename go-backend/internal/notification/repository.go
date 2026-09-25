package notification

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/notification/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) List(ctx context.Context, userID int64, limit int) ([]Item, error) {
	rows, err := r.queries.ListNotifications(ctx, query.ListNotificationsParams{UserID: userID, Limit: int32(limit)})
	if err != nil {
		return nil, err
	}
	items := make([]Item, 0, len(rows))
	for _, row := range rows {
		items = append(items, Item{
			ID: row.ID, Source: row.Source, Title: row.Title, Body: row.Body,
			TargetPath: row.TargetPath, Priority: row.Priority, CreatedAt: row.CreatedAt.Time.UTC(),
		})
	}
	return items, nil
}

func (r *Repository) Dismiss(ctx context.Context, userID, id int64) (bool, error) {
	count, err := r.queries.DismissNotification(ctx, query.DismissNotificationParams{ID: id, UserID: userID})
	return count > 0, err
}

func (r *Repository) Clear(ctx context.Context, userID int64) error {
	return r.queries.ClearNotifications(ctx, userID)
}

func (r *Repository) CreateGymReminder(ctx context.Context, tx pgx.Tx, userID int64, date string) (int64, error) {
	return r.queries.WithTx(tx).CreateGymReminder(ctx, query.CreateGymReminderParams{UserID: userID, Column2: date})
}

func (r *Repository) HasGymReminder(ctx context.Context, tx pgx.Tx, userID, id int64) (bool, error) {
	return r.queries.WithTx(tx).GymReminderExists(ctx, query.GymReminderExistsParams{ID: id, UserID: userID})
}

func (r *Repository) DismissGymReminder(ctx context.Context, tx pgx.Tx, id int64) error {
	return r.queries.WithTx(tx).DismissGymReminder(ctx, id)
}

func (r *Repository) CreateAirFillReminder(ctx context.Context, tx pgx.Tx, userID, vehicleID int64, name string) (int64, error) {
	return r.queries.WithTx(tx).CreateAirFillReminder(ctx, query.CreateAirFillReminderParams{
		UserID: userID, Column2: vehicleID, Column3: &name,
	})
}
