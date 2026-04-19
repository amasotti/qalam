#!/usr/bin/env python3
"""
Phase 1 — Step 3: Migrate words.example → word_examples table
Expected: 4 rows (only non-empty examples)

Content is mixed Arabic/transliteration raw text — spot-check after migration.
"""
import os
import uuid

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT id, example FROM words
        WHERE example IS NOT NULL AND example <> ''
    """)
    rows = src.fetchall()
    for (word_id, example) in rows:
        dst.execute("""
            INSERT INTO word_examples (id, word_id, arabic, transliteration, translation, created_at)
            VALUES (%s, %s, %s, NULL, NULL, now())
        """, (str(uuid.uuid4()), word_id, example))
    new.commit()

print(f"Inserted {len(rows)} word examples")
