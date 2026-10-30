BEGIN;
UPDATE plan_places SET time_pinned = false WHERE time_pinned IS NULL;
ALTER TABLE plan_places ALTER COLUMN time_pinned SET DEFAULT false;
ALTER TABLE plan_places ALTER COLUMN time_pinned SET NOT NULL;
COMMIT;
