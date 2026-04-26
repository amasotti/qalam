-- One-to-one morphological metadata per word.
-- gender and verb_pattern are nullable — not all words have both.

CREATE TABLE word_morphology (
    word_id      UUID PRIMARY KEY REFERENCES words(id) ON DELETE CASCADE,
    gender       VARCHAR(12) CHECK (gender IN ('MASCULINE', 'FEMININE')),
    verb_pattern VARCHAR(5)  CHECK (verb_pattern IN ('I','II','III','IV','V','VI','VII','VIII','IX','X'))
);
