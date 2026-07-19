-- Migrate existing verb_pattern data from word_morphology into verb_details,
-- then drop the column from word_morphology.

INSERT INTO verb_details (word_id, verb_form, weakness_type)
SELECT word_id, verb_pattern, 'SOUND'
FROM word_morphology
WHERE verb_pattern IS NOT NULL
ON CONFLICT (word_id) DO NOTHING;

ALTER TABLE word_morphology DROP COLUMN verb_pattern;
