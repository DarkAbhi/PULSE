\set ON_ERROR_STOP on
\getenv statement_test_password STATEMENT_TEST_PASSWORD
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO users (username, password_hash)
VALUES ('statement-test-user', crypt(:'statement_test_password', gen_salt('bf', 10)))
ON CONFLICT (username) DO NOTHING;
