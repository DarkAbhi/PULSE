package gym

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"strconv"
	"testing"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestCatalogResolution(t *testing.T) {
	candidate := func(id, match string) catalogCandidate {
		return catalogCandidate{catalogExercise: catalogExercise{ID: id}, MatchType: match}
	}
	for _, tc := range []struct {
		name       string
		candidates []catalogCandidate
		match, id  string
	}{
		{"unknown", []catalogCandidate{}, "none", ""},
		{"suggestion is not a match", []catalogCandidate{candidate("machine", "suggested")}, "suggested", ""},
		{"unique exact", []catalogCandidate{candidate("press", "exact"), candidate("other", "suggested")}, "exact", "press"},
		{"confirmed alias", []catalogCandidate{candidate("plate", "alias")}, "alias", "plate"},
		{"duplicate names", []catalogCandidate{candidate("a", "exact"), candidate("b", "exact")}, "ambiguous", ""},
	} {
		t.Run(tc.name, func(t *testing.T) {
			result := catalogSearchResult("original name", tc.candidates)
			id := ""
			if result.ExerciseCatalogID != nil {
				id = *result.ExerciseCatalogID
			}
			if result.MatchType != tc.match || id != tc.id || result.Query != "original name" {
				t.Fatalf("unexpected resolution: %+v", result)
			}
		})
	}
	if got := normalizeExerciseName("  Dumbbell\t Shoulder   PRESS  "); got != "dumbbell shoulder press" {
		t.Fatalf("normalization: %q", got)
	}
}

func TestExerciseCatalog(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	h := newTestHandler(t, db, dsn)
	router := chi.NewRouter()
	h.RegisterRoutes(router)
	cookie := loginUser(t, db)
	var visitID int64
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES (1) RETURNING id`).Scan(&visitID); err != nil {
		t.Fatal(err)
	}

	// Verify the migration against pre-catalogue history, including an unknown name.
	rollback, err := os.ReadFile("../../migrations/00023_exercise_catalog.down.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(string(rollback)); err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO gym_visit_exercises (gym_visit_id, name) VALUES ($1, '  DUMBBELL  Shoulder Press '), ($1, 'My custom exercise')`, visitID); err != nil {
		t.Fatal(err)
	}
	seed, err := os.ReadFile("../../migrations/00023_exercise_catalog.up.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(string(seed)); err != nil {
		t.Fatal(err)
	}
	var total, linked int
	if err := db.QueryRow(`SELECT COUNT(*) FROM exercise_catalog`).Scan(&total); err != nil || total != 876 {
		t.Fatalf("catalogue count=%d err=%v", total, err)
	}
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visit_exercises WHERE exercise_catalog_id IS NOT NULL`).Scan(&linked); err != nil || linked != 1 {
		t.Fatalf("backfill count=%d err=%v", linked, err)
	}

	request := func(method, path string, body any, authenticated bool) *httptest.ResponseRecorder {
		t.Helper()
		payload, err := json.Marshal(body)
		if err != nil {
			t.Fatal(err)
		}
		req := httptest.NewRequest(method, path, bytes.NewReader(payload))
		if authenticated {
			req.AddCookie(cookie)
		}
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		return rec
	}
	search := func(name string) catalogSearch {
		t.Helper()
		rec := request(http.MethodGet, "/exercise-catalog?q="+url.QueryEscape(name), nil, true)
		if rec.Code != http.StatusOK {
			t.Fatalf("search status=%d: %s", rec.Code, rec.Body.String())
		}
		var result catalogSearch
		if err := json.Unmarshal(rec.Body.Bytes(), &result); err != nil {
			t.Fatal(err)
		}
		return result
	}
	if rec := request(http.MethodGet, "/exercise-catalog?q=press", nil, false); rec.Code != http.StatusUnauthorized {
		t.Fatalf("search without session: %d", rec.Code)
	}
	if rec := request(http.MethodGet, "/exercise-catalog?q=", nil, true); rec.Code != http.StatusBadRequest {
		t.Fatalf("empty search: %d", rec.Code)
	}
	exact := search("  DUMBBELL   SHOULDER press  ")
	if exact.MatchType != "exact" || exact.ExerciseCatalogID == nil || *exact.ExerciseCatalogID != "Dumbbell_Shoulder_Press" {
		t.Fatalf("exact resolution: %+v", exact)
	}
	if result := search("rear delt fly"); result.ExerciseCatalogID != nil {
		t.Fatalf("inferred an ambiguous variant: %+v", result)
	}

	plateID := "Front_Plate_Raise"
	weight := 10.0
	plate := createExerciseBody{
		Name: "weight plate front raises", ExerciseCatalogID: &plateID, RememberAlias: true,
		Sets: []exerciseSetInput{{Reps: 10, Weight: &weight}},
	}
	path := "/gym-visits/" + strconv.FormatInt(visitID, 10) + "/exercises"
	missingID := "does-not-exist"
	invalid := createExerciseBody{Name: "invalid", ExerciseCatalogID: &missingID, Sets: plate.Sets}
	rec := request(http.MethodPost, path+"/batch", createExercisesBody{Exercises: []createExerciseBody{plate, invalid}}, true)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("invalid ID: %d: %s", rec.Code, rec.Body.String())
	}
	if result := search(plate.Name); result.MatchType == "alias" {
		t.Fatal("rejected batch remembered an alias")
	}

	tooHeavy := 1000000.0
	invalid.ExerciseCatalogID = nil
	invalid.Sets = []exerciseSetInput{{Reps: 10, Weight: &tooHeavy}}
	rec = request(http.MethodPost, path+"/batch", createExercisesBody{Exercises: []createExerciseBody{plate, invalid}}, true)
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("expected database insert failure: %d: %s", rec.Code, rec.Body.String())
	}
	if result := search(plate.Name); result.MatchType == "alias" {
		t.Fatal("rolled-back batch remembered an alias")
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visit_exercises WHERE gym_visit_id=$1`, visitID).Scan(&count); err != nil || count != 2 {
		t.Fatalf("partial batch was saved: count=%d err=%v", count, err)
	}

	if rec := request(http.MethodPost, path, plate, true); rec.Code != http.StatusCreated {
		t.Fatalf("single catalogue save: %d: %s", rec.Code, rec.Body.String())
	}
	if result := search(" Weight  Plate Front Raises "); result.MatchType != "alias" || *result.ExerciseCatalogID != plateID {
		t.Fatalf("alias resolution: %+v", result)
	}
	conflicting := plate
	conflicting.Name = "Dumbbell Shoulder Press"
	if rec := request(http.MethodPost, path, conflicting, true); rec.Code != http.StatusBadRequest {
		t.Fatalf("alias replaced an exact name: %d: %s", rec.Code, rec.Body.String())
	}
	manual := plate
	manual.Name = "my plate lift"
	manual.RememberAlias = false
	if rec := request(http.MethodPost, path, manual, true); rec.Code != http.StatusCreated {
		t.Fatalf("manual match: %d: %s", rec.Code, rec.Body.String())
	}
	if result := search(manual.Name); result.MatchType == "alias" {
		t.Fatal("remembered a name without opting in")
	}
	var otherUserID int64
	if err := db.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('catalog-other','test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	other, err := h.service.SearchCatalog(t.Context(), otherUserID, plate.Name)
	if err != nil || other.MatchType == "alias" {
		t.Fatalf("alias leaked to another user: %+v err=%v", other, err)
	}
	batch := createExercisesBody{Exercises: []createExerciseBody{
		{Name: plate.Name, Sets: plate.Sets},
		{Name: "Dumbbell Shoulder Press", Sets: plate.Sets},
		{Name: "My custom exercise", Sets: plate.Sets},
	}}
	rec = request(http.MethodPost, path+"/batch", batch, true)
	if rec.Code != http.StatusCreated {
		t.Fatalf("batch save: %d: %s", rec.Code, rec.Body.String())
	}
	var saved []exerciseDTO
	if err := json.Unmarshal(rec.Body.Bytes(), &saved); err != nil {
		t.Fatal(err)
	}
	if len(saved) != 3 || saved[0].ExerciseCatalogID == nil || *saved[0].ExerciseCatalogID != plateID ||
		saved[0].Name != plate.Name || saved[0].CatalogExercise == nil || saved[1].ExerciseCatalogID == nil ||
		saved[2].ExerciseCatalogID != nil || saved[2].CatalogExercise != nil {
		t.Fatalf("unexpected catalogue save: %+v", saved)
	}
	rec = request(http.MethodGet, path, nil, true)
	if rec.Code != http.StatusOK || !bytes.Contains(rec.Body.Bytes(), []byte(`"primaryMuscles"`)) {
		t.Fatalf("catalogue metadata missing from history: %d: %s", rec.Code, rec.Body.String())
	}
}
