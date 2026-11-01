BEGIN;

-- These retired features have no application readers or writers; their data is discarded.
DROP TABLE meditations, credit_cards, sports;

COMMIT;
