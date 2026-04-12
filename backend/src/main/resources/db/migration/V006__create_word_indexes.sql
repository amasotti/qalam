-- Indexes for vocabulary filtering and full-text / fuzzy search.

-- B-tree indexes for equality filtering
CREATE INDEX idx_words_root_id        ON words (root_id);
CREATE INDEX idx_words_derived_from   ON words (derived_from_id);
CREATE INDEX idx_words_mastery        ON words (mastery_level);
CREATE INDEX idx_words_dialect        ON words (dialect);
CREATE INDEX idx_words_difficulty     ON words (difficulty);
CREATE INDEX idx_words_part_of_speech ON words (part_of_speech);

-- pg_trgm GIN indexes for fuzzy / ILIKE search
CREATE INDEX idx_words_arabic_trgm      ON words USING gin (arabic_text gin_trgm_ops);
CREATE INDEX idx_words_translation_trgm ON words USING gin (translation gin_trgm_ops);
