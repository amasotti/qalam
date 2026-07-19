ALTER TABLE conjugation_exercise_sessions
    ADD COLUMN exercise_type VARCHAR(20) NOT NULL DEFAULT 'MATCH_FORM'
    CHECK (exercise_type IN ('MATCH_FORM', 'WRITE_FORM'));
