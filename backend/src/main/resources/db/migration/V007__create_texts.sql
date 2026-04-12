-- Arabic texts — the primary learning content.
-- Plain view only in M5; sentences and interlinear gloss are added in M6.

CREATE TABLE texts (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           TEXT        NOT NULL,
    body            TEXT        NOT NULL,
    transliteration TEXT,
    translation     TEXT,
    difficulty      VARCHAR(20) NOT NULL DEFAULT 'BEGINNER'
                    CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    dialect         VARCHAR(20) NOT NULL DEFAULT 'MSA'
                    CHECK (dialect IN ('TUNISIAN','MOROCCAN','EGYPTIAN','GULF',
                                       'LEVANTINE','MSA','IRAQI')),
    comments        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER texts_updated_at
    BEFORE UPDATE ON texts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Filter columns used by the list endpoint
CREATE INDEX idx_texts_difficulty ON texts (difficulty);
CREATE INDEX idx_texts_dialect    ON texts (dialect);

-- Full-text search across title and body (used by ?q= filter)
CREATE INDEX idx_texts_title_trgm ON texts USING gin (title gin_trgm_ops);
CREATE INDEX idx_texts_body_trgm  ON texts USING gin (body  gin_trgm_ops);
