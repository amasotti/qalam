#!/usr/bin/env python3
"""
Phase 1 — Step 6: Migrate texts + interlinear_texts → unified texts table
Expected: 42 rows (24 plain + 18 interlinear)

Plain texts: arabic_content → body, tags JSONB dropped (handled by script 7)
Interlinear texts: body assembled from ordered sentences, difficulty defaults to BEGINNER
"Tradizioni di Natale" exists in both source tables — both kept (different UUIDs, different content)
"""
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with (
    old.cursor() as src_texts,
    old.cursor() as src_inter,
    old.cursor() as src_sents,
    new.cursor() as dst,
):
    # Plain texts
    src_texts.execute("""
        SELECT id, title, arabic_content, transliteration, translation,
               difficulty, dialect, comments, created_at
        FROM texts
    """)
    plain_count = 0
    for (id_, title, body, translit, trans, diff, dial, comments, cat) in src_texts.fetchall():
        dst.execute("""
            INSERT INTO texts (id, title, body, transliteration, translation,
                               difficulty, dialect, comments, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (id_, title, body, translit, trans, diff, dial, comments, cat, cat))
        plain_count += 1

    # Interlinear texts — assemble body from sentences
    src_inter.execute("""
        SELECT id, title, description, dialect, created_at
        FROM interlinear_texts
    """)
    inter_count = 0
    for (id_, title, desc, dial, cat) in src_inter.fetchall():
        src_sents.execute("""
            SELECT arabic_text FROM interlinear_sentences
            WHERE text_id = %s
            ORDER BY sentence_order
        """, (id_,))
        sentences = [r[0] for r in src_sents.fetchall()]
        body = "\n".join(sentences) if sentences else ""
        dst.execute("""
            INSERT INTO texts (id, title, body, transliteration, translation,
                               difficulty, dialect, comments, created_at, updated_at)
            VALUES (%s, %s, %s, NULL, NULL, 'BEGINNER', %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (id_, title, body, dial, desc, cat, cat))
        inter_count += 1

    new.commit()

print(f"Inserted {plain_count} plain texts + {inter_count} interlinear texts = {plain_count + inter_count} total")
