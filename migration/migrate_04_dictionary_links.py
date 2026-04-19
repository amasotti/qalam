#!/usr/bin/env python3
"""
Phase 1 — Step 4: Migrate dictionary_links → word_dictionary_links
Expected: 1,458 rows

display_name column dropped (no target). Original UUIDs preserved.
"""
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("SELECT id, word_id, type, url FROM dictionary_links")
    rows = src.fetchall()
    dst.executemany("""
        INSERT INTO word_dictionary_links (id, word_id, source, url)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (id) DO NOTHING
    """, rows)
    new.commit()

print(f"Inserted {len(rows)} dictionary links")
