package vehicle

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type airFillNotifier interface {
	CreateAirFillReminder(context.Context, pgx.Tx, int64, int64, string) (int64, error)
}
type ReminderService struct {
	db            *pgxpool.Pool
	notifications airFillNotifier
}

func NewReminderService(db *pgxpool.Pool, notifications airFillNotifier) *ReminderService {
	return &ReminderService{db: db, notifications: notifications}
}

func (s *ReminderService) CreateDue(ctx context.Context) error {
	rows, err := s.db.Query(ctx, `SELECT id FROM vehicle_air_fills WHERE reminder_notification_id IS NULL AND filled_at <= NOW() - INTERVAL '30 days' LIMIT 100`)
	if err != nil {
		return fmt.Errorf("list due air fills: %w", err)
	}
	ids := make([]int64, 0)
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err != nil {
			rows.Close()
			return err
		}
		ids = append(ids, id)
	}
	err = rows.Err()
	rows.Close()
	if err != nil {
		return err
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
	var userID, vehicleID int64
	var name string
	err = tx.QueryRow(ctx, `SELECT vehicle_air_fills.user_id,vehicle_air_fills.vehicle_id,vehicles.name FROM vehicle_air_fills JOIN vehicles ON vehicles.id=vehicle_air_fills.vehicle_id WHERE vehicle_air_fills.id=$1 AND vehicle_air_fills.reminder_notification_id IS NULL AND vehicle_air_fills.filled_at <= NOW()-INTERVAL '30 days' FOR UPDATE`, id).Scan(&userID, &vehicleID, &name)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil
	}
	if err != nil {
		return err
	}
	notificationID, err := s.notifications.CreateAirFillReminder(ctx, tx, userID, vehicleID, name)
	if err != nil {
		return err
	}
	if _, err := tx.Exec(ctx, `UPDATE vehicle_air_fills SET reminder_notification_id=$1 WHERE id=$2`, notificationID, id); err != nil {
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
