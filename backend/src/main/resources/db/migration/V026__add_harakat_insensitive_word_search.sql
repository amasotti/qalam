-- PostgreSQL's bundled unaccent dictionary does not remove Arabic harakat.
-- Keep this immutable so PostgreSQL can use it in the trigram expression index below.
CREATE FUNCTION remove_arabic_diacritics(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
STRICT
RETURN translate(
    input,
    U&'\0610\0611\0612\0613\0614\0615\0616\0617\0618\0619\061A\064B\064C\064D\064E\064F\0650\0651\0652\0653\0654\0655\0656\0657\0658\0659\065A\065B\065C\065D\065E\065F\0670\06D6\06D7\06D8\06D9\06DA\06DB\06DC\06DF\06E0\06E1\06E2\06E3\06E4\06E5\06E6\06E7\06E8\06EA\06EB\06EC\06ED',
    ''
);

CREATE INDEX words_arabic_text_without_harakat_trgm
    ON words USING GIN (remove_arabic_diacritics(arabic_text) gin_trgm_ops);
