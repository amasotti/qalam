-- Sentences within a text — ordered list, position is 1-based.
-- tokensValid tracks whether alignment tokens are current with arabicText.

CREATE TABLE sentences (
    id               UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    text_id          UUID        NOT NULL REFERENCES texts(id) ON DELETE CASCADE,
    position         INT         NOT NULL,
    arabic_text      TEXT        NOT NULL,
    transliteration  TEXT,
    free_translation TEXT,
    notes            TEXT,
    tokens_valid     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (text_id, position)
);

CREATE TRIGGER sentences_updated_at
    BEFORE UPDATE ON sentences
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_sentences_text_id ON sentences (text_id);
