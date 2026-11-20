package timeutil

import (
	"testing"
	"time"
)

func TestDayBoundsIndia(t *testing.T) {
	// A specific time: 2026-07-18 10:00:00 UTC
	// In India (UTC+5:30), this is 2026-07-18 15:30:00
	t1 := time.Date(2026, 7, 18, 10, 0, 0, 0, time.UTC)
	start1, end1 := DayBoundsIndia(t1)

	// In India, start of 2026-07-18 is 2026-07-18 00:00:00 IST (2026-07-17 18:30:00 UTC)
	// End of 2026-07-18 is 2026-07-19 00:00:00 IST (2026-07-18 18:30:00 UTC)
	expectedStart1 := time.Date(2026, 7, 17, 18, 30, 0, 0, time.UTC)
	expectedEnd1 := time.Date(2026, 7, 18, 18, 30, 0, 0, time.UTC)

	if !start1.Equal(expectedStart1) {
		t.Errorf("expected start %v, got %v", expectedStart1, start1)
	}
	if !end1.Equal(expectedEnd1) {
		t.Errorf("expected end %v, got %v", expectedEnd1, end1)
	}

	// Another test case: 2026-07-18 20:00:00 UTC
	// In India, this is 2026-07-19 01:30:00 (next day)
	t2 := time.Date(2026, 7, 18, 20, 0, 0, 0, time.UTC)
	start2, end2 := DayBoundsIndia(t2)

	// In India, start of 2026-07-19 is 2026-07-19 00:00:00 IST (2026-07-18 18:30:00 UTC)
	// End of 2026-07-19 is 2026-07-20 00:00:00 IST (2026-07-19 18:30:00 UTC)
	expectedStart2 := time.Date(2026, 7, 18, 18, 30, 0, 0, time.UTC)
	expectedEnd2 := time.Date(2026, 7, 19, 18, 30, 0, 0, time.UTC)

	if !start2.Equal(expectedStart2) {
		t.Errorf("expected start %v, got %v", expectedStart2, start2)
	}
	if !end2.Equal(expectedEnd2) {
		t.Errorf("expected end %v, got %v", expectedEnd2, end2)
	}
}

func TestNextMonthDate(t *testing.T) {
	dateStr := NextMonthDate()
	parsed, err := time.Parse("2006-01-02", dateStr)
	if err != nil {
		t.Fatalf("failed to parse next month date string %q: %v", dateStr, err)
	}

	loc, err := time.LoadLocation(IndiaTimeZone)
	if err != nil {
		loc = time.UTC
	}
	now := time.Now().UTC().In(loc)
	expectedMonth := now.Month() + 1
	expectedYear := now.Year()
	if expectedMonth > 12 {
		expectedMonth = 1
		expectedYear++
	}

	if parsed.Year() != expectedYear || parsed.Month() != expectedMonth || parsed.Day() != 1 {
		t.Errorf("expected first day of next month (%d-%02d-01), got %v", expectedYear, expectedMonth, dateStr)
	}
}
