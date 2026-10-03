package gym

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestDeleteVisitExercise(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	cookie := loginUser(t, db)
	router := chi.NewRouter()
	newTestHandler(t, db, dsn).RegisterRoutes(router)
	service := newTestService(t, dsn)
	ctx := context.Background()
	visitID, err := service.AddVisit(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	otherVisitID, err := service.AddVisit(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	catalogID := "Front_Plate_Raise"
	saved, err := service.CreateExercises(ctx, 1, visitID, []createExerciseBody{
		{Name: "My plate raise", ExerciseCatalogID: &catalogID, RememberAlias: true,
			Sets: []exerciseSetInput{{Reps: 10}, {Reps: 12}}},
		{Name: "Custom exercise", Sets: []exerciseSetInput{{Reps: 8}}},
	})
	if err != nil {
		t.Fatal(err)
	}
	var otherUserID int64
	if err := db.QueryRow(`INSERT INTO users (username, password_hash)
		VALUES ('exercise-delete-other', 'test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	foreignVisitID, err := service.AddVisit(ctx, otherUserID)
	if err != nil {
		t.Fatal(err)
	}
	foreignExercise, err := service.CreateExercise(ctx, otherUserID, foreignVisitID,
		createExerciseBody{Name: "Foreign exercise", Sets: []exerciseSetInput{{Reps: 5}}})
	if err != nil {
		t.Fatal(err)
	}

	for _, tc := range []struct {
		name, visit, exercise string
		authenticated         bool
		want                  int
	}{
		{"no session", fmt.Sprint(visitID), fmt.Sprint(saved[0].ID), false, 401},
		{"invalid visit", "0", fmt.Sprint(saved[0].ID), true, 400},
		{"invalid exercise", fmt.Sprint(visitID), "bad", true, 400},
		{"nonpositive exercise", fmt.Sprint(visitID), "0", true, 400},
		{"wrong workout", fmt.Sprint(otherVisitID), fmt.Sprint(saved[0].ID), true, 404},
		{"another user's exercise", fmt.Sprint(foreignVisitID), fmt.Sprint(foreignExercise.ID), true, 404},
		{"missing exercise", fmt.Sprint(visitID), "9223372036854775807", true, 404},
		{"delete", fmt.Sprint(visitID), fmt.Sprint(saved[0].ID), true, 204},
		{"already deleted", fmt.Sprint(visitID), fmt.Sprint(saved[0].ID), true, 404},
	} {
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodDelete,
				"/gym-visits/"+tc.visit+"/exercises/"+tc.exercise, nil)
			if tc.authenticated {
				req.AddCookie(cookie)
			}
			rec := httptest.NewRecorder()
			router.ServeHTTP(rec, req)
			if rec.Code != tc.want {
				t.Fatalf("status %d, want %d: %s", rec.Code, tc.want, rec.Body.String())
			}
		})
	}
	remaining, err := service.ListExercises(ctx, 1, visitID)
	if err != nil || len(remaining) != 1 || remaining[0].ID != saved[1].ID || len(remaining[0].Sets) != 1 {
		t.Fatalf("remaining exercise changed: %+v, err=%v", remaining, err)
	}
	foreign, err := service.ListExercises(ctx, otherUserID, foreignVisitID)
	if err != nil || len(foreign) != 1 || len(foreign[0].Sets) != 1 {
		t.Fatalf("foreign exercise changed: %+v, err=%v", foreign, err)
	}
	var setCount int
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_exercise_sets WHERE gym_visit_exercise_id=$1`,
		saved[0].ID).Scan(&setCount); err != nil || setCount != 0 {
		t.Fatalf("sets did not cascade: count=%d, err=%v", setCount, err)
	}
	match, err := service.SearchCatalog(ctx, 1, "My plate raise")
	if err != nil || match.ExerciseCatalogID == nil || *match.ExerciseCatalogID != catalogID {
		t.Fatalf("catalogue or remembered alias changed: %+v, err=%v", match, err)
	}
}
