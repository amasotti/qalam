#!/usr/bin/env python3
"""
Phase 1 — Step 1: Migrate arabic_roots
Expected: 145 rows inserted
"""
import json
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT id, letters, normalized_form, display_form, letter_count,
               NULLIF(meaning, ''), analysis, created_at, updated_at
        FROM arabic_roots
    """)
    rows = src.fetchall()
    for (id_, letters_json, norm, disp, lc, meaning, analysis, cat, uat) in rows:
        # psycopg2 auto-deserialises jsonb → Python list; no json.loads needed
        letters_list = letters_json if isinstance(letters_json, list) else json.loads(letters_json)
        letters_pg = "{" + ",".join(f'"{l}"' for l in letters_list) + "}"
        dst.execute("""
            INSERT INTO arabic_roots
                (id, letters, normalized_form, display_form, letter_count, meaning, analysis, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (normalized_form) DO NOTHING
        """, (id_, letters_pg, norm, disp, lc, meaning, analysis, cat, uat))
    new.commit()

print(f"Inserted {len(rows)} roots")
