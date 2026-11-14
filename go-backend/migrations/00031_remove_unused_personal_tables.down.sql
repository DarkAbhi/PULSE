BEGIN;

-- Restore the schema only; dropped records require a backup to recover.
CREATE TABLE meditations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX meditations_user_created_idx ON meditations (user_id, created_at DESC, id DESC);

CREATE TABLE credit_cards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    link TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX credit_cards_user_created_idx ON credit_cards (user_id, created_at DESC, id DESC);

CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL CHECK (name IN ('cricket', 'football', 'badminton')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX sports_user_created_idx ON sports (user_id, created_at DESC, id DESC);

COMMIT;
