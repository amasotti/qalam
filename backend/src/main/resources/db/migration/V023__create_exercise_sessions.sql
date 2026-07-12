CREATE TABLE exercise_sessions (
    id              UUID PRIMARY KEY,
    mode            VARCHAR(20) NOT NULL
                      CHECK (mode IN ('NEW', 'LEARNING', 'KNOWN', 'MIXED')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'COMPLETED')),
    total_items     INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    skipped_count   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE exercise_session_items (
    id                  UUID PRIMARY KEY,
    session_id          UUID NOT NULL REFERENCES exercise_sessions(id) ON DELETE CASCADE,
    word_id             UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    position            INT NOT NULL,
    type                VARCHAR(40) NOT NULL
                          CHECK (type IN (
                              'MULTIPLE_CHOICE_MEANING',
                              'MULTIPLE_CHOICE_ARABIC',
                              'CONFUSABLE_MEANING',
                              'CONFUSABLE_ARABIC'
                          )),
    prompt_kind         VARCHAR(20) NOT NULL
                          CHECK (prompt_kind IN ('ARABIC_WORD', 'TRANSLATION')),
    prompt_text         TEXT NOT NULL,
    result              VARCHAR(20) CHECK (result IN ('CORRECT', 'INCORRECT', 'SKIPPED')),
    selected_option_id  UUID,
    answered_at         TIMESTAMP WITH TIME ZONE,
    mastery_promoted_to VARCHAR(20),
    UNIQUE (session_id, position)
);

CREATE TABLE exercise_item_options (
    id              UUID PRIMARY KEY,
    item_id         UUID NOT NULL REFERENCES exercise_session_items(id) ON DELETE CASCADE,
    word_id         UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    position        INT NOT NULL,
    arabic_text     TEXT NOT NULL,
    transliteration TEXT,
    translation     TEXT,
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (item_id, position)
);

CREATE INDEX idx_exercise_sessions_status ON exercise_sessions(status);
CREATE INDEX idx_exercise_sessions_created_at ON exercise_sessions(created_at DESC);
CREATE INDEX idx_exercise_items_session_id ON exercise_session_items(session_id);
CREATE INDEX idx_exercise_items_word_id ON exercise_session_items(word_id);
CREATE INDEX idx_exercise_options_item_id ON exercise_item_options(item_id);
CREATE INDEX idx_exercise_options_word_id ON exercise_item_options(word_id);
