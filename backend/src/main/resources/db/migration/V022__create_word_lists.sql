-- Curated topical vocabulary lists (e.g. "Colors", "Family").
-- A list references existing words; membership is many-to-many and insertion-ordered.

CREATE TABLE word_lists (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER word_lists_updated_at
    BEFORE UPDATE ON word_lists
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- One row per (list, word). position preserves insertion order within a list.
CREATE TABLE word_list_items (
    list_id  UUID        NOT NULL REFERENCES word_lists(id) ON DELETE CASCADE,
    word_id  UUID        NOT NULL REFERENCES words(id)      ON DELETE CASCADE,
    position INT         NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (list_id, word_id),
    UNIQUE (list_id, position)
);

-- Reverse lookup: which lists a word belongs to (word-detail membership).
CREATE INDEX idx_wli_word_id       ON word_list_items (word_id);
-- Ordered member fetch for a list.
CREATE INDEX idx_wli_list_position ON word_list_items (list_id, position);
