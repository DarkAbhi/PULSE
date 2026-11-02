package vehicle

import (
	"testing"
	"time"
)

func TestCalculateUpcoming(t *testing.T) {
	instant := func(s string) time.Time {
		v, err := time.Parse(time.RFC3339, s)
		if err != nil {
			t.Fatal(err)
		}
		return v
	}
	now := instant("2026-11-02T06:00:00Z")
	changed := instant("2026-09-07T09:34:00Z")
	newer := instant("2026-10-01T09:34:00Z")
	records := []maintenanceRecord{
		{ID: 1, Category: "service", OccurredAt: changed, OdometerKM: floatPtr(4722), Actions: []maintenanceAction{{Component: "engine_oil", Action: "replaced"}, {Component: "general_service", Action: "completed"}}},
		{ID: 2, Category: "service", OccurredAt: newer, OdometerKM: floatPtr(6000), Actions: []maintenanceAction{{Component: "engine_oil", Action: "topped_up"}}},
	}
	result := calculateUpcoming("motorcycle", []string{"petrol"}, []airFillHistory{{FilledAt: now.AddDate(0, 0, -18)}}, nil, records, now)
	oil, air, service := result[0], result[1], result[2]
	if *oil.DueDate != "2027-01-07" || *oil.RemainingKM != 1722 || !oil.LastAt.Equal(changed) {
		t.Fatalf("top-up reset oil: %+v", oil)
	}
	if air.Status != "overdue" || *air.RemainingDays != -4 {
		t.Fatalf("pressure: %+v", air)
	}
	if *service.DueDate != "2027-03-07" || !service.LastAt.Equal(changed) {
		t.Fatalf("oil-only service reset general service: %+v", service)
	}
	t.Run("mileage first and capped progress", func(t *testing.T) {
		fills := []fuelFillHistory{{FilledAt: now, OdometerKM: 8000}}
		oil := calculateUpcoming("motorcycle", []string{"petrol"}, nil, fills, records, now)[0]
		if oil.Status != "overdue" || *oil.RemainingKM != -278 || *oil.Progress != 1 {
			t.Fatalf("oil: %+v", oil)
		}
	})
	t.Run("missing history and electric", func(t *testing.T) {
		items := calculateUpcoming("car", []string{"electric"}, nil, nil, nil, now)
		if len(items) != 2 || items[0].Kind != "tire_pressure" || items[1].Status != "no_history" || items[1].Progress != nil {
			t.Fatalf("items: %+v", items)
		}
		unknown := calculateUpcoming("unknown", []string{"petrol"}, nil, nil, nil, now)
		if len(unknown) != 1 {
			t.Fatalf("unknown type got engine schedule: %+v", unknown)
		}
	})
	t.Run("month end and time only", func(t *testing.T) {
		records := []maintenanceRecord{{ID: 1, OccurredAt: instant("2026-10-31T06:00:00Z"), Actions: []maintenanceAction{{Component: "engine_oil", Action: "replaced"}}}}
		oil := calculateUpcoming("moped", []string{"petrol"}, nil, nil, records, now)[0]
		if *oil.DueDate != "2027-02-28" || oil.RemainingKM != nil {
			t.Fatalf("month clamp: %+v", oil)
		}
	})
	t.Run("future and regressing mileage ignored", func(t *testing.T) {
		records := append(append([]maintenanceRecord{}, records...), maintenanceRecord{ID: 3, OccurredAt: now.AddDate(0, 0, 1), OdometerKM: floatPtr(9000), Actions: []maintenanceAction{{Component: "engine_oil", Action: "replaced"}}})
		oil := calculateUpcoming("motorcycle", []string{"petrol"}, nil, []fuelFillHistory{{FilledAt: now, OdometerKM: 100}}, records, now)[0]
		if !oil.LastAt.Equal(changed) || oil.RemainingKM != nil {
			t.Fatalf("invalid baseline: %+v", oil)
		}
	})
}

func TestNormalizeMaintenanceActions(t *testing.T) {
	cases := []struct {
		name    string
		actions []maintenanceAction
		invalid bool
	}{
		{"empty", nil, false},
		{"normalize", []maintenanceAction{{Component: " Engine_Oil ", Action: " Replaced ", Description: " oil "}}, false},
		{"unknown action", []maintenanceAction{{Component: "oil", Action: "changed"}}, true},
		{"invalid key", []maintenanceAction{{Component: "oil;drop", Action: "replaced"}}, true},
		{"duplicate", []maintenanceAction{{Component: "oil", Action: "replaced"}, {Component: "oil", Action: "replaced"}}, true},
		{"adjustment and replacement", []maintenanceAction{{Component: "clutch_cable", Action: "adjusted"}, {Component: "clutch_cable", Action: "replaced"}}, false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if err := normalizeMaintenanceActions(&tc.actions); (err != nil) != tc.invalid {
				t.Fatalf("err=%v", err)
			}
		})
	}
}
