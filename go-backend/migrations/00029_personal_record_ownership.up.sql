BEGIN;

-- Pre-populated owner columns allow an operator-reviewed mapping on multi-user installations.
ALTER TABLE meditations ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE meditations SET user_id = (SELECT min(id) FROM users)
WHERE user_id IS NULL AND (SELECT count(*) FROM users) = 1;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM meditations WHERE user_id IS NULL) THEN
        RAISE EXCEPTION 'meditations has ambiguous ownership; map historical user_id values before migrating';
    END IF;
END $$;

ALTER TABLE meditations
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT meditations_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX meditations_user_created_idx ON meditations (user_id, created_at DESC, id DESC);

ALTER TABLE credit_cards ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE credit_cards SET user_id = (SELECT min(id) FROM users)
WHERE user_id IS NULL AND (SELECT count(*) FROM users) = 1;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM credit_cards WHERE user_id IS NULL) THEN
        RAISE EXCEPTION 'credit_cards has ambiguous ownership; map historical user_id values before migrating';
    END IF;
END $$;

ALTER TABLE credit_cards
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT credit_cards_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX credit_cards_user_created_idx ON credit_cards (user_id, created_at DESC, id DESC);

ALTER TABLE sports ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE sports SET user_id = (SELECT min(id) FROM users)
WHERE user_id IS NULL AND (SELECT count(*) FROM users) = 1;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM sports WHERE user_id IS NULL) THEN
        RAISE EXCEPTION 'sports has ambiguous ownership; map historical user_id values before migrating';
    END IF;
END $$;

ALTER TABLE sports
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT sports_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX sports_user_created_idx ON sports (user_id, created_at DESC, id DESC);

COMMIT;
