// Package notification creates and manages user reminders.
package notification

import "time"

type Item struct {
	ID         int64
	Source     string
	Title      string
	Body       *string
	TargetPath *string
	Priority   int16
	CreatedAt  time.Time
}
