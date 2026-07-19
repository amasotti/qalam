-- Verb-specific metadata: form, vowel patterns, weakness classification.
-- Separated from word_morphology (which retains only gender for nouns/adjectives).

CREATE TABLE verb_details (
    word_id         UUID PRIMARY KEY REFERENCES words(id) ON DELETE CASCADE,
    verb_form       VARCHAR(5)  NOT NULL CHECK (verb_form IN ('I','II','III','IV','V','VI','VII','VIII','IX','X')),
    past_pattern    VARCHAR(20),
    present_pattern VARCHAR(20),
    weakness_type   VARCHAR(20) NOT NULL DEFAULT 'SOUND'
                    CHECK (weakness_type IN ('SOUND','ASSIMILATED','HOLLOW','GEMINATE','DEFECTIVE','DOUBLY_WEAK')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_verb_details_form ON verb_details(verb_form);
CREATE INDEX idx_verb_details_weakness ON verb_details(weakness_type);
