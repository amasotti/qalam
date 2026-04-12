CREATE TABLE annotations (
    id            UUID        PRIMARY KEY,
    text_id       UUID        NOT NULL REFERENCES texts(id) ON DELETE CASCADE,
    anchor        VARCHAR     NOT NULL,
    type          VARCHAR     NOT NULL CHECK (type IN ('VOCAB','GRAMMAR','CULTURAL','STRUCTURE')),
    content       TEXT,
    mastery_level VARCHAR     CHECK (mastery_level IN ('NEW','LEARNING','KNOWN','MASTERED')),
    review_flag   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_annotations_text_id ON annotations(text_id);
