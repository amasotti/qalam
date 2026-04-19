#!/usr/bin/env python3
"""
Phase 1 — Step 5: Migrate word_progress_tracking → word_progress
Expected: 181 tracked + 50 default = 231 rows

50 words have no tracking row — zero-default rows created for them.
Dropped: id, mastery_level_updated_at, created_at, updated_at
"""
import os

import psycopg2

old = psycopg2.connect(os.environ["OLD_DSN"])
new = psycopg2.connect(os.environ["NEW_DSN"])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at
        FROM word_progress_tracking
    """)
    tracked = src.fetchall()
    tracked_ids = tuple(r[0] for r in tracked)

    dst.executemany("""
        INSERT INTO word_progress (word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (word_id) DO NOTHING
    """, tracked)

    dst.execute("SELECT id FROM words WHERE id NOT IN %s", (tracked_ids,))
    untracked = dst.fetchall()
    dst.executemany("""
        INSERT INTO word_progress (word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at)
        VALUES (%s, 0, 0, 0, NULL)
        ON CONFLICT (word_id) DO NOTHING
    """, [(r[0],) for r in untracked])

    new.commit()

print(f"Inserted {len(tracked)} tracked + {len(untracked)} default word_progress rows")
