CREATE TABLE conjugation_exercise_sessions (
    id              UUID PRIMARY KEY,
    mode            VARCHAR(20) NOT NULL
                    CHECK (mode IN ('NEW', 'LEARNING', 'KNOWN', 'MIXED')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'COMPLETED')),
    tense           VARCHAR(20) NOT NULL
                    CHECK (tense IN ('PAST', 'PRESENT')),
    voice           VARCHAR(20) NOT NULL
                    CHECK (voice IN ('ACTIVE', 'PASSIVE')),
    total_items     INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    skipped_count   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE conjugation_exercise_items (
    id                  UUID PRIMARY KEY,
    session_id          UUID NOT NULL REFERENCES conjugation_exercise_sessions(id) ON DELETE CASCADE,
    word_id             UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    position            INT NOT NULL,
    lemma_snapshot      TEXT NOT NULL,
    translation_snapshot TEXT,
    verb_form_snapshot  VARCHAR(5) NOT NULL,
    result              VARCHAR(20) CHECK (result IN ('CORRECT', 'INCORRECT', 'SKIPPED')),
    answered_at         TIMESTAMP WITH TIME ZONE,
    UNIQUE (session_id, position)
);

CREATE TABLE conjugation_exercise_pairs (
    id              UUID PRIMARY KEY,
    item_id         UUID NOT NULL REFERENCES conjugation_exercise_items(id) ON DELETE CASCADE,
    position        INT NOT NULL,
    form_id         UUID NOT NULL UNIQUE,
    label_id        UUID NOT NULL UNIQUE,
    arabic          TEXT NOT NULL,
    segments_json   JSONB NOT NULL,
    tense           VARCHAR(20) NOT NULL CHECK (tense IN ('PAST', 'PRESENT')),
    voice           VARCHAR(20) NOT NULL CHECK (voice IN ('ACTIVE', 'PASSIVE')),
    person          VARCHAR(10) NOT NULL CHECK (person IN (
                        '1S', '2SM', '2SF', '3SM', '3SF', '2D', '3DM',
                        '3DF', '1P', '2PM', '2PF', '3PM', '3PF'
                    )),
    UNIQUE (item_id, position)
);

CREATE TABLE conjugation_exercise_answers (
    item_id           UUID NOT NULL REFERENCES conjugation_exercise_items(id) ON DELETE CASCADE,
    form_id           UUID NOT NULL,
    selected_label_id UUID,
    submitted_text    TEXT,
    is_correct        BOOLEAN NOT NULL,
    PRIMARY KEY (item_id, form_id),
    CHECK (selected_label_id IS NOT NULL OR submitted_text IS NOT NULL)
);

CREATE INDEX idx_conjugation_exercise_sessions_status ON conjugation_exercise_sessions(status);
CREATE INDEX idx_conjugation_exercise_sessions_created_at ON conjugation_exercise_sessions(created_at DESC);
CREATE INDEX idx_conjugation_exercise_items_session_id ON conjugation_exercise_items(session_id);
CREATE INDEX idx_conjugation_exercise_items_word_id ON conjugation_exercise_items(word_id);
CREATE INDEX idx_conjugation_exercise_pairs_item_id ON conjugation_exercise_pairs(item_id);
CREATE INDEX idx_conjugation_exercise_answers_item_id ON conjugation_exercise_answers(item_id);
