-- Vocabulary words — the core of the learning experience.
-- Each word has optional links to a root and to a morphological parent (derivedFrom).

CREATE TABLE words (
    id                UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    arabic_text       TEXT        NOT NULL,
    transliteration   TEXT,
    translation       TEXT,
    example_sentence  TEXT,
    part_of_speech    VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
                      CHECK (part_of_speech IN ('UNKNOWN','NOUN','VERB','ADJECTIVE','ADVERB',
                                                'PREPOSITION','PARTICLE','INTERJECTION',
                                                'CONJUNCTION','PRONOUN')),
    dialect           VARCHAR(20) NOT NULL DEFAULT 'MSA'
                      CHECK (dialect IN ('TUNISIAN','MOROCCAN','EGYPTIAN','GULF',
                                         'LEVANTINE','MSA','IRAQI')),
    difficulty        VARCHAR(20) NOT NULL DEFAULT 'BEGINNER'
                      CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    mastery_level     VARCHAR(20) NOT NULL DEFAULT 'NEW'
                      CHECK (mastery_level IN ('NEW','LEARNING','KNOWN','MASTERED')),
    pronunciation_url TEXT,
    root_id           UUID        REFERENCES arabic_roots(id) ON DELETE SET NULL,
    derived_from_id   UUID        REFERENCES words(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER words_updated_at
    BEFORE UPDATE ON words
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
