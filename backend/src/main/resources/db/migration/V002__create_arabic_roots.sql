-- Arabic roots: the foundational morphological units from which vocabulary is derived.
-- Each root is 2–6 consonants; letters stored as a typed TEXT array for easy querying.

CREATE TABLE arabic_roots (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    letters         TEXT[]      NOT NULL,
    normalized_form VARCHAR(12) NOT NULL UNIQUE,   -- e.g. "رحب"
    display_form    VARCHAR(24) NOT NULL,           -- e.g. "ر-ح-ب"
    letter_count    SMALLINT    NOT NULL CHECK (letter_count BETWEEN 2 AND 6),
    meaning         TEXT,
    analysis        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_roots_letter_count ON arabic_roots (letter_count);
CREATE INDEX idx_roots_normalized   ON arabic_roots (normalized_form);
