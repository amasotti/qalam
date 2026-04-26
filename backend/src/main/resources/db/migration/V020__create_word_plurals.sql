-- Plural forms for a word; a word may have more than one plural.
-- plural_type defaults to BROKEN (the most common Arabic plural type).

CREATE TABLE word_plurals (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    word_id     UUID        NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    plural_form TEXT        NOT NULL,
    plural_type VARCHAR(20) NOT NULL DEFAULT 'BROKEN'
                CHECK (plural_type IN ('SOUND_MASC','SOUND_FEM','BROKEN','PAUCAL','COLLECTIVE','OTHER'))
);

CREATE INDEX idx_word_plurals_word_id ON word_plurals(word_id);
