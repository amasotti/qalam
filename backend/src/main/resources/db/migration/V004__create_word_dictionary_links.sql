-- External dictionary links per word.
-- Multiple sources per word; cascade-delete when word is deleted.

CREATE TABLE word_dictionary_links (
    id      UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    word_id UUID        NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    source  VARCHAR(30) NOT NULL
            CHECK (source IN ('ALMANY','LIVING_ARABIC','DERJA_NINJA','REVERSO',
                              'WIKTIONARY','ARABIC_STUDENT_DICTIONARY',
                              'LANGENSCHEIDT','CUSTOM')),
    url     TEXT        NOT NULL
);

CREATE INDEX idx_word_dictionary_links_word_id ON word_dictionary_links (word_id);
