//go:build integration

package gym

import (
	"context"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestRepositoryScopesExerciseChildren(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	var otherID int64
	if err := db.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('child-other','unused') RETURNING id`).Scan(&otherID); err != nil {
		t.Fatal(err)
	}
	repo := NewRepository(pool)
	visitID, err := repo.AddVisit(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	body := createExerciseBody{Name: "private exercise", Sets: []exerciseSetInput{{Reps: 5}}}
	second := createExerciseBody{Name: "second exercise", Sets: []exerciseSetInput{{Reps: 8}, {Reps: 10}}}
	if _, err := repo.CreateExercises(ctx, 1, visitID, []createExerciseBody{body, second}); err != nil {
		t.Fatal(err)
	}
	if items, err := repo.ListExercises(ctx, otherID, visitID); err != nil || len(items) != 0 {
		t.Fatalf("foreign children visible: %+v %v", items, err)
	}
	if _, err := repo.CreateExercises(ctx, otherID, visitID, []createExerciseBody{body}); err == nil {
		t.Fatal("foreign child insert accepted")
	}
	items, err := repo.ListExercises(ctx, 1, visitID)
	if err != nil || len(items) != 2 || len(items[0].Sets) != 1 || items[0].Sets[0].Reps != 5 || len(items[1].Sets) != 2 || items[1].Sets[1].Reps != 10 {
		t.Fatalf("foreign insert modified owner's exercises: %+v %v", items, err)
	}
}
