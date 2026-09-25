package notification

import "time"

type Notification struct {
	ID         int64
	Source     string
	Title      string
	Body       *string
	TargetPath *string
	Priority   int16
	CreatedAt  time.Time
}
