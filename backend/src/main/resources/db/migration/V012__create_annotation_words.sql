CREATE TABLE annotation_words (
    annotation_id UUID NOT NULL REFERENCES annotations(id) ON DELETE CASCADE,
    word_id       UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    PRIMARY KEY (annotation_id, word_id)
);
CREATE INDEX idx_annotation_words_annotation_id ON annotation_words(annotation_id);
CREATE INDEX idx_annotation_words_word_id ON annotation_words(word_id);
