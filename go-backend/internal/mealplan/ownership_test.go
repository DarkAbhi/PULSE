package mealplan

import (
	"context"
	"errors"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestMealTimeOwnership(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	var otherID, timeID int64
	if err := db.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('meal-other','unused') RETURNING id`).Scan(&otherID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO meal_times (user_id,name,start_time,end_time) VALUES ($1,'private meal time','08:00','09:00') RETURNING id`, otherID).Scan(&timeID); err != nil {
		t.Fatal(err)
	}
	service := NewService(NewRepository(pool))
	if _, err := service.CreatePlan(ctx, 1, CreatePlanInput{Date: "2026-10-01", Name: "attempt", MealTimeID: &timeID}); !errors.Is(err, ErrTimeNotFound) {
		t.Fatalf("foreign meal time accepted: %v", err)
	}
	var planID int64
	if err := db.QueryRow(`INSERT INTO meal_plans (user_id,date,name,meal_time_id) VALUES (1,'2026-10-01','historical malformed link',$1) RETURNING id`, timeID).Scan(&planID); err != nil {
		t.Fatal(err)
	}
	plans, err := service.ListPlans(ctx, 1, "2026-10-01", "", "")
	if err != nil || len(plans) != 1 {
		t.Fatalf("list plans: %+v %v", plans, err)
	}
	if plans[0].MealTimeName != nil || plans[0].StartTime != nil || plans[0].EndTime != nil {
		t.Fatalf("foreign meal time exposed: %+v", plans[0])
	}
	updated, err := service.SetConsumed(ctx, 1, planID, true)
	if err != nil || updated.MealTimeName != nil {
		t.Fatalf("consumed response exposed foreign meal time: %+v %v", updated, err)
	}
}
