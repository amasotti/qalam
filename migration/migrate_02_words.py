#!/usr/bin/env python3
"""
Phase 1 — Step 2: Migrate words
Expected: 231 words inserted, 3 derived_from links patched

Self-referential FK: insert all rows with derived_from_id=NULL first, then back-fill.
"""
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT id, arabic, transliteration, translation, part_of_speech,
               difficulty, dialect, mastery_level, pronunciation_link,
               root_id, derived_from, created_at
        FROM words
    """)
    rows = src.fetchall()
    derived_pairs = []

    for (id_, arabic, translit, trans, pos, diff, dial, mastery,
         pron_link, root_id, derived_from, cat) in rows:
        if derived_from:
            derived_pairs.append((id_, derived_from))
        dst.execute("""
            INSERT INTO words
                (id, arabic_text, transliteration, translation, part_of_speech,
                 difficulty, dialect, mastery_level, pronunciation_url, root_id,
                 derived_from_id, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NULL, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (id_, arabic, translit, trans, pos, diff, dial, mastery,
              pron_link, root_id, cat, cat))

    for child_id, parent_id in derived_pairs:
        dst.execute(
            "UPDATE words SET derived_from_id = %s WHERE id = %s",
            (parent_id, child_id),
        )
    new.commit()

print(f"Inserted {len(rows)} words, patched {len(derived_pairs)} derived_from links")
