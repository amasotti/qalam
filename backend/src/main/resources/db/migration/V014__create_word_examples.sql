-- Structured example sentences for vocabulary words.
-- Each word can have multiple examples, each with arabic, transliteration and translation.

CREATE TABLE word_examples (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    word_id         UUID        NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    arabic          TEXT        NOT NULL,
    transliteration TEXT,
    translation     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_word_examples_word_id ON word_examples(word_id);
