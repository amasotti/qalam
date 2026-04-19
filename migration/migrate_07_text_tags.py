#!/usr/bin/env python3
"""
Phase 1 — Step 7: Expand texts.tags JSONB array → text_tags rows
Expected: ~57 rows from 24 plain texts (interlinear_texts have no tags)
"""
import json
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("SELECT id, tags FROM texts WHERE tags IS NOT NULL AND tags != '[]'")
    total = 0
    for (text_id, tags_json) in src.fetchall():
        tags = tags_json if isinstance(tags_json, list) else json.loads(tags_json)
        for tag in tags:
            tag = tag.strip()
            if tag:
                dst.execute("""
                    INSERT INTO text_tags (text_id, tag) VALUES (%s, %s)
                    ON CONFLICT DO NOTHING
                """, (text_id, tag))
                total += 1
    new.commit()

print(f"Inserted {total} text_tags rows")
