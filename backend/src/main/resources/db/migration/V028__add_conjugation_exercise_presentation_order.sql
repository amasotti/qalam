ALTER TABLE conjugation_exercise_pairs
    ADD COLUMN form_position INT NOT NULL,
    ADD COLUMN label_position INT NOT NULL;

CREATE UNIQUE INDEX idx_conjugation_exercise_pairs_form_position
    ON conjugation_exercise_pairs(item_id, form_position);

CREATE UNIQUE INDEX idx_conjugation_exercise_pairs_label_position
    ON conjugation_exercise_pairs(item_id, label_position);
