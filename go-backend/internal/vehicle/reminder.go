package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type airFillNotifier interface {
	CreateAirFillReminder(context.Context, pgx.Tx, int64, int64, string) (int64, error)
}
type ReminderService struct {
	db            *pgxpool.Pool
	notifications airFillNotifier
	queries       *query.Queries
}

func NewReminderService(db *pgxpool.Pool, notifications airFillNotifier) *ReminderService {
	return &ReminderService{db: db, notifications: notifications, queries: query.New(db)}
}

func (s *ReminderService) CreateDue(ctx context.Context) error {
	ids, err := s.queries.ListDueAirFills(ctx)
	if err != nil {
		return fmt.Errorf("list due air fills: %w", err)
	}
	var failures []error
	for _, id := range ids {
		if err := s.create(ctx, id); err != nil {
			failures = append(failures, fmt.Errorf("air fill %d: %w", id, err))
		}
	}
	return errors.Join(failures...)
}
func (s *ReminderService) create(ctx context.Context, id int64) error {
	tx, err := s.db.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	q := s.queries.WithTx(tx)
	fill, err := q.LockDueAirFill(ctx, id)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil
	}
	if err != nil {
		return err
	}
	notificationID, err := s.notifications.CreateAirFillReminder(ctx, tx, fill.UserID, fill.VehicleID, fill.Name)
	if err != nil {
		return err
	}
	if err := q.MarkAirFillReminderSent(ctx, query.MarkAirFillReminderSentParams{ReminderNotificationID: sql.NullInt64{Int64: notificationID, Valid: true}, ID: id}); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
func RunAirFillReminderJob(ctx context.Context, service *ReminderService) {
	run := func() {
		if err := service.CreateDue(ctx); err != nil {
			slog.Error("air-fill reminder job failed", "error", err)
		}
	}
	run()
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			run()
		}
	}
}
