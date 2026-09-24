package gym

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"time"

	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

// RunGymReminderJob creates the weekday gym reminder at 3:30 PM India time.
// It also catches up after a restart later on the same eligible day.
func RunGymReminderJob(database *sql.DB) {
	createDueGymReminders(database, time.Now().UTC())
	ticker := time.NewTicker(time.Minute)
	defer ticker.Stop()
	for now := range ticker.C {
		createDueGymReminders(database, now)
	}
}

func createDueGymReminders(database *sql.DB, now time.Time) {
	location, err := time.LoadLocation(timeutil.IndiaTimeZone)
	if err != nil {
		log.Printf("gym reminder timezone load failed: %v", err)
		return
	}
	localNow := now.In(location)
	if localNow.Weekday() == time.Sunday || localNow.Hour() < 15 || (localNow.Hour() == 15 && localNow.Minute() < 30) {
		return
	}

	reminderDate := localNow.Format("2006-01-02")
	rows, err := sqlc.New(database).ListGymReminderUsers(context.Background())
	if err != nil {
		log.Printf("gym reminder user scan failed: %v", err)
		return
	}

	for _, userID := range rows {
		if err := createGymReminder(database, userID, reminderDate); err != nil {
			log.Printf("gym reminder failed for user %d: %v", userID, err)
		}
	}
}

func createGymReminder(database *sql.DB, userID int64, reminderDate string) error {
	tx, err := database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	q := sqlc.New(tx)

	notificationID, err := q.CreateGymReminderNotification(context.Background(), sqlc.CreateGymReminderNotificationParams{UserID: userID, Source: gymReminderSource, Column3: reminderDate})
	if errors.Is(err, sql.ErrNoRows) {
		return nil
	}
	if err != nil {
		return err
	}
	if err := q.RecordGymReminderDelivery(context.Background(), sqlc.RecordGymReminderDeliveryParams{UserID: userID, Column2: reminderDate, NotificationID: notificationID}); err != nil {
		return err
	}
	return tx.Commit()
}
