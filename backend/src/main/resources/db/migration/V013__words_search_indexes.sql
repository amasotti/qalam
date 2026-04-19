-- pg_trgm extension already enabled (V001).
-- Index all three text search columns so ILIKE '%q%' is index-backed.
CREATE INDEX words_arabic_text_trgm   ON words USING GIN (arabic_text    gin_trgm_ops);
CREATE INDEX words_translation_trgm   ON words USING GIN (translation     gin_trgm_ops);
CREATE INDEX words_transliteration_trgm ON words USING GIN (transliteration gin_trgm_ops);
