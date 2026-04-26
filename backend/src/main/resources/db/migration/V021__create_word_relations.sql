-- Directed lexical relations between words (synonym, antonym, related).
-- Composite PK prevents duplicate (word, related, type) triples.
-- no_self_relation guards against a word pointing to itself.

CREATE TABLE word_relations (
    word_id         UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    related_word_id UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    relation_type   VARCHAR(10) NOT NULL CHECK (relation_type IN ('SYNONYM','ANTONYM','RELATED')),
    PRIMARY KEY (word_id, related_word_id, relation_type),
    CONSTRAINT no_self_relation CHECK (word_id <> related_word_id)
);

CREATE INDEX idx_word_relations_related ON word_relations(related_word_id);
