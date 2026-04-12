-- SRS progress tracking — 1:1 with words.
-- Created atomically in application code when a word is created.

CREATE TABLE word_progress (
    word_id             UUID        PRIMARY KEY REFERENCES words(id) ON DELETE CASCADE,
    consecutive_correct INT         NOT NULL DEFAULT 0,
    total_attempts      INT         NOT NULL DEFAULT 0,
    total_correct       INT         NOT NULL DEFAULT 0,
    last_reviewed_at    TIMESTAMPTZ
);
