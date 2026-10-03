package gym

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"reflect"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestUpdateVisitExercise(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	service := newTestService(t, dsn)
	cookie := loginUser(t, db)
	router := chi.NewRouter()
	newTestHandler(t, db, dsn).RegisterRoutes(router)
	ctx := context.Background()
	visitID, err := service.AddVisit(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	otherVisitID, err := service.AddVisit(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	original, err := service.CreateExercises(ctx, 1, visitID, []createExerciseBody{
		{Name: "Custom raises", Sets: []exerciseSetInput{{Reps: 10}, {Reps: 12}}},
		{Name: "Leave alone", Sets: []exerciseSetInput{{Reps: 8}}},
	})
	if err != nil {
		t.Fatal(err)
	}
	var otherUserID int64
	if err := db.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('edit-other','test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	foreignVisitID, err := service.AddVisit(ctx, otherUserID)
	if err != nil {
		t.Fatal(err)
	}
	foreign, err := service.CreateExercise(ctx, otherUserID, foreignVisitID,
		createExerciseBody{Name: "Foreign", Sets: []exerciseSetInput{{Reps: 5}}})
	if err != nil {
		t.Fatal(err)
	}
	put := func(visit, exercise int64, body createExerciseBody, authenticated bool) int {
		t.Helper()
		data, err := json.Marshal(body)
		if err != nil {
			t.Fatal(err)
		}
		req := httptest.NewRequest(http.MethodPut, fmt.Sprintf("/gym-visits/%d/exercises/%d", visit, exercise), bytes.NewReader(data))
		if authenticated {
			req.AddCookie(cookie)
		}
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		return rec.Code
	}
	catalogID := "Front_Plate_Raise"
	weight := 15.5
	body := createExerciseBody{Name: "My front raises", ExerciseCatalogID: &catalogID, RememberAlias: true,
		Sets: []exerciseSetInput{{Reps: 9, Weight: &weight}}}
	for _, tc := range []struct {
		name            string
		visit, exercise int64
		auth            bool
		want            int
	}{
		{"unauthenticated", visitID, original[0].ID, false, 401},
		{"wrong workout", otherVisitID, original[0].ID, true, 404},
		{"another user", foreignVisitID, foreign.ID, true, 404},
		{"missing", visitID, 9223372036854775807, true, 404},
		{"invalid id", visitID, 0, true, 400},
	} {
		t.Run(tc.name, func(t *testing.T) {
			if got := put(tc.visit, tc.exercise, body, tc.auth); got != tc.want {
				t.Fatalf("status=%d want=%d", got, tc.want)
			}
		})
	}
	invalid := body
	invalid.Sets = []exerciseSetInput{{Reps: 0}}
	if got := put(visitID, original[0].ID, invalid, true); got != 400 {
		t.Fatalf("invalid reps status=%d", got)
	}
	missingID := "does-not-exist"
	invalid = body
	invalid.ExerciseCatalogID = &missingID
	if got := put(visitID, original[0].ID, invalid, true); got != 400 {
		t.Fatalf("invalid catalogue status=%d", got)
	}
	tooLarge := 1000000.0
	invalid = body
	invalid.Sets = []exerciseSetInput{{Reps: 9}, {Reps: 10, Weight: &tooLarge}}
	if got := put(visitID, original[0].ID, invalid, true); got != 500 {
		t.Fatalf("failed set write status=%d", got)
	}
	unchanged, err := service.ListExercises(ctx, 1, visitID)
	if err != nil || !reflect.DeepEqual(original, unchanged) {
		t.Fatalf("failed updates changed original: %+v err=%v", unchanged, err)
	}
	match, err := service.SearchCatalog(ctx, 1, body.Name)
	if err != nil || match.ExerciseCatalogID != nil {
		t.Fatalf("failed updates saved alias: %+v err=%v", match, err)
	}
	if got := put(visitID, original[0].ID, body, true); got != 204 {
		t.Fatalf("update status=%d", got)
	}
	saved, err := service.ListExercises(ctx, 1, visitID)
	if err != nil || len(saved) != 2 {
		t.Fatalf("saved: %+v err=%v", saved, err)
	}
	if saved[0].ID != original[0].ID || saved[0].Name != body.Name || saved[0].CatalogExercise == nil ||
		saved[0].CatalogExercise.ID != catalogID || len(saved[0].Sets) != 1 || saved[0].Sets[0].SetNumber != 1 ||
		saved[0].Sets[0].Reps != 9 || *saved[0].Sets[0].Weight != weight || !reflect.DeepEqual(saved[1], original[1]) {
		t.Fatalf("incorrect update: %+v", saved)
	}
	match, err = service.SearchCatalog(ctx, 1, body.Name)
	if err != nil || match.ExerciseCatalogID == nil || *match.ExerciseCatalogID != catalogID {
		t.Fatalf("alias not saved: %+v err=%v", match, err)
	}
	// Explicit null must unlink even an exact catalogue name.
	body.Name = "Front Plate Raise"
	body.ExerciseCatalogID = nil
	body.RememberAlias = false
	if got := put(visitID, original[0].ID, body, true); got != 204 {
		t.Fatalf("unlink status=%d", got)
	}
	saved, err = service.ListExercises(ctx, 1, visitID)
	if err != nil || saved[0].ExerciseCatalogID != nil {
		t.Fatalf("unlink failed: %+v err=%v", saved, err)
	}
	foreignSaved, err := service.ListExercises(ctx, otherUserID, foreignVisitID)
	if err != nil || len(foreignSaved) != 1 || !reflect.DeepEqual(foreignSaved[0], foreign) {
		t.Fatalf("foreign exercise changed: %+v err=%v", foreignSaved, err)
	}
}
