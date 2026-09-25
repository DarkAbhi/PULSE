package gym

import (
	"context"
	"log/slog"
	"time"
)

func RunReminderJob(ctx context.Context, service *Service) {
	run := func(now time.Time) {
		if err := service.CreateDueReminders(ctx, now); err != nil {
			slog.Error("gym reminder job failed", "error", err)
		}
	}
	run(time.Now().UTC())
	ticker := time.NewTicker(time.Minute)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case now := <-ticker.C:
			run(now)
		}
	}
}
