-- Migrate existing type values to new enum names
UPDATE annotations SET type = 'VOCABULARY' WHERE type = 'VOCAB';
UPDATE annotations SET type = 'OTHER'      WHERE type = 'STRUCTURE';

-- Drop vestigial columns (inline CHECK constraints on these columns go with them)
ALTER TABLE annotations DROP COLUMN IF EXISTS mastery_level;
ALTER TABLE annotations DROP COLUMN IF EXISTS review_flag;

-- Replace the type CHECK constraint (auto-named annotations_type_check by Postgres)
DO $$
DECLARE
    con_name text;
BEGIN
    SELECT con.conname INTO con_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'annotations'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%VOCAB%';
    IF con_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE annotations DROP CONSTRAINT ' || quote_ident(con_name);
    END IF;
END $$;

ALTER TABLE annotations ADD CONSTRAINT annotations_type_check
    CHECK (type IN ('VOCABULARY', 'GRAMMAR', 'CULTURAL', 'OTHER'));
