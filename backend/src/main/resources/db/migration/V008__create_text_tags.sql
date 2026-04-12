-- Tags for texts — simple join table, one row per (text, tag) pair.
-- No surrogate PK; the composite is sufficient.

CREATE TABLE text_tags (
    text_id UUID        NOT NULL REFERENCES texts(id) ON DELETE CASCADE,
    tag     VARCHAR(100) NOT NULL,
    PRIMARY KEY (text_id, tag)
);

-- Index for tag-based filtering (?tag= on the list endpoint)
CREATE INDEX idx_text_tags_tag ON text_tags (tag);
