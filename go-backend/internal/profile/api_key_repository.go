package profile

import "context"

func (r *Repository) CreateAPIKey(ctx context.Context, userID int64, name, hash, prefix string) (APIKey, error) {
	var key APIKey
	err := r.db.QueryRow(ctx, `
		INSERT INTO api_keys (user_id, name, token_hash, token_prefix)
		VALUES ($1, $2, $3, $4)
		RETURNING id, name, token_prefix, created_at`, userID, name, hash, prefix,
	).Scan(&key.ID, &key.Name, &key.TokenPrefix, &key.CreatedAt)
	return key, err
}

func (r *Repository) ListAPIKeys(ctx context.Context, userID int64) ([]APIKey, error) {
	rows, err := r.db.Query(ctx, `
		SELECT id, name, token_prefix, created_at FROM api_keys
		WHERE user_id = $1 AND revoked_at IS NULL
		ORDER BY created_at DESC, id DESC`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	keys := []APIKey{}
	for rows.Next() {
		var key APIKey
		if err := rows.Scan(&key.ID, &key.Name, &key.TokenPrefix, &key.CreatedAt); err != nil {
			return nil, err
		}
		keys = append(keys, key)
	}
	return keys, rows.Err()
}

func (r *Repository) RevokeAPIKey(ctx context.Context, userID, keyID int64) (bool, error) {
	result, err := r.db.Exec(ctx, `
		UPDATE api_keys SET revoked_at = NOW()
		WHERE id = $1 AND user_id = $2 AND revoked_at IS NULL`, keyID, userID)
	return result.RowsAffected() > 0, err
}
