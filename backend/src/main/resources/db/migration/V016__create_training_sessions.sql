CREATE TABLE training_sessions (
    id              UUID PRIMARY KEY,
    mode            VARCHAR(20) NOT NULL
                      CHECK (mode IN ('NEW', 'LEARNING', 'KNOWN', 'MIXED')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'COMPLETED')),
    total_words     INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    skipped_count   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE training_session_words (
    id                  UUID PRIMARY KEY,
    session_id          UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    word_id             UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    position            INT  NOT NULL,
    front_side          VARCHAR(20) NOT NULL
                          CHECK (front_side IN ('ARABIC', 'TRANSLATION')),
    result              VARCHAR(20) CHECK (result IN ('CORRECT', 'INCORRECT', 'SKIPPED')),
    answered_at         TIMESTAMP WITH TIME ZONE,
    mastery_promoted_to VARCHAR(20),
    UNIQUE (session_id, position)
);

CREATE INDEX idx_tsw_session_id ON training_session_words(session_id);
CREATE INDEX idx_tsw_word_id    ON training_session_words(word_id);
CREATE INDEX idx_ts_status      ON training_sessions(status);
CREATE INDEX idx_ts_created_at  ON training_sessions(created_at DESC);
