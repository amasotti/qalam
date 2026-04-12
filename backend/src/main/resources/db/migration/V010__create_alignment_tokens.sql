-- Alignment tokens for a sentence — one row per word-level token.
-- word_id is optional; links to the vocabulary table when known.

CREATE TABLE alignment_tokens (
    id              UUID  PRIMARY KEY DEFAULT uuid_generate_v4(),
    sentence_id     UUID  NOT NULL REFERENCES sentences(id) ON DELETE CASCADE,
    position        INT   NOT NULL,
    arabic          TEXT  NOT NULL,
    transliteration TEXT,
    translation     TEXT,
    word_id         UUID  REFERENCES words(id) ON DELETE SET NULL,
    UNIQUE (sentence_id, position)
);

CREATE INDEX idx_alignment_tokens_sentence_id ON alignment_tokens (sentence_id);
