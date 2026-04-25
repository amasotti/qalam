-- V006 created idx_words_arabic_trgm and idx_words_translation_trgm.
-- V013 added the same columns plus transliteration, under different names.
-- Drop the V006 duplicates; V013 indexes remain authoritative.
DROP INDEX IF EXISTS idx_words_arabic_trgm;
DROP INDEX IF EXISTS idx_words_translation_trgm;
