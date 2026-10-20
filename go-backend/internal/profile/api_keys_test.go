package profile

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

type apiKeyStore struct {
	owner int64
	hash  string
	key   APIKey
}

func TestAPIKeyRepositoryScopesRevocation(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	if _, err := db.Exec(`INSERT INTO users (username, password_hash) VALUES ('another-user', 'unused')`); err != nil {
		t.Fatal(err)
	}
	var anotherID int64
	if err := db.QueryRow(`SELECT id FROM users WHERE username = 'another-user'`).Scan(&anotherID); err != nil {
		t.Fatal(err)
	}
	service := NewService(NewRepository(pool), nil)
	key, token, err := service.CreateAPIKey(context.Background(), 1, "Sync")
	if err != nil {
		t.Fatal(err)
	}
	var storedHash string
	if err := db.QueryRow(`SELECT token_hash FROM api_keys WHERE id = $1`, key.ID).Scan(&storedHash); err != nil {
		t.Fatal(err)
	}
	hash := sha256.Sum256([]byte(token))
	if storedHash != hex.EncodeToString(hash[:]) {
		t.Fatal("database did not store the token hash")
	}
	if revoked, err := service.RevokeAPIKey(context.Background(), anotherID, key.ID); err != nil || revoked {
		t.Fatalf("other user revoked key: revoked=%v err=%v", revoked, err)
	}
	if keys, err := service.ListAPIKeys(context.Background(), anotherID); err != nil || len(keys) != 0 {
		t.Fatalf("other user can list key: keys=%v err=%v", keys, err)
	}
	if revoked, err := service.RevokeAPIKey(context.Background(), 1, key.ID); err != nil || !revoked {
		t.Fatalf("owner could not revoke key: revoked=%v err=%v", revoked, err)
	}
	if keys, err := service.ListAPIKeys(context.Background(), 1); err != nil || len(keys) != 0 {
		t.Fatalf("revoked key remained active: keys=%v err=%v", keys, err)
	}
}

func (s *apiKeyStore) Name(context.Context, int64) (string, error) { return "", nil }
func (s *apiKeyStore) Save(context.Context, int64, string) error   { return nil }
func (s *apiKeyStore) CreateAPIKey(_ context.Context, owner int64, name, hash, prefix string) (APIKey, error) {
	s.owner, s.hash = owner, hash
	s.key = APIKey{ID: 7, Name: name, TokenPrefix: prefix}
	return s.key, nil
}
func (s *apiKeyStore) ListAPIKeys(context.Context, int64) ([]APIKey, error) {
	return []APIKey{s.key}, nil
}
func (s *apiKeyStore) RevokeAPIKey(_ context.Context, owner, keyID int64) (bool, error) {
	return owner == s.owner && keyID == s.key.ID, nil
}

func TestAPIKeyCreationAndOwnership(t *testing.T) {
	store := &apiKeyStore{}
	service := NewService(store, nil)
	key, token, err := service.CreateAPIKey(context.Background(), 1, " Sync service ")
	if err != nil {
		t.Fatal(err)
	}
	hash := sha256.Sum256([]byte(token))
	if store.hash != hex.EncodeToString(hash[:]) || store.hash == token || store.owner != 1 {
		t.Fatal("key was not stored as a hash for its owner")
	}
	if key.Name != "Sync service" || key.TokenPrefix != token[:11] {
		t.Fatal("key metadata is incorrect")
	}
	if revoked, _ := service.RevokeAPIKey(context.Background(), 2, key.ID); revoked {
		t.Fatal("another user revoked the key")
	}
	if revoked, _ := service.RevokeAPIKey(context.Background(), 1, key.ID); !revoked {
		t.Fatal("owner could not revoke the key")
	}
}
