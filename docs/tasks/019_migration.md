## Milestone 19 — Data Migration from an-na7wi

Import existing data from the old Postgres instance into qalam.
Executed in two phases: Phase 1 is MVP (get data in to start daily use), Phase 2 is post-MVP.

---

### Phase 1 — MVP: Core data (roots, vocabulary, texts)

Run after M15 (vocabulary frontend) is live. Enough to replace an-na7wi for daily vocabulary review.

- [ ] 19.1 `[I]` Restore an-na7wi dump to a temp DB; document all field renames and structural changes (see ROADMAP.md quick-reference)
- [ ] 19.2 `[I]` Write SQL migration script — Phase 1 only:
  - `arabic_roots` (no deps)
  - `words` (resolve root_id FK from step above)
  - `word_dictionary_links` (expand JSON column → rows)
  - `word_progress` (rename table, drop `mastery_level_updated_at`)
  - `texts` (unified: merge `interlinear_texts` by title match; `arabic_content` → `body`; drop version columns)
  - `text_tags` (expand JSONB array → rows)
- [ ] 19.3 `[I]` `just migrate-from-old F=<old_dump>` recipe: restore old dump to temp DB → run script → verify counts
- [ ] 19.4 `[I]` Validation queries: row counts match; FK integrity checks pass; spot-check 20 random words and 5 texts in the new UI

### Phase 2 — Post-MVP: Sentences, annotations, training

Run after M7 (annotations backend) and M16 (texts frontend) are stable.

- [ ] 19.5 `[I]` Extend migration script:
  - `sentences` (from `interlinear_sentences`: `sentence_order`→`position`, `translation`→`free_translation`, `annotations`→`notes`)
  - `alignment_tokens` (from `word_alignments`: spot-check multi-word token rows before splitting)
  - `annotations` + `annotation_words`
  - `training_sessions` + `training_session_words`
- [ ] 19.6 `[I]` Full validation pass: all FK integrity checks, spot-check interlinear view for 3 migrated texts

---

## Detailed Phase 1 Plan

### Source data summary (dump: `annahwi_20260419_123249.sql`)

| Source table              | Rows  | Notes                                                                 |
|---------------------------|-------|-----------------------------------------------------------------------|
| `arabic_roots`            | 145   | All 3- or 4-letter roots; no 5/6-letter roots present                 |
| `words`                   | 231   | 40 have root_id set; 3 have derived_from set                          |
| `dictionary_links`        | 1,458 | ~6.3 links/word avg; 7 source types (no CUSTOM in this dump)          |
| `word_progress_tracking`  | 181   | 50 words have no tracking row — see step 4 notes                      |
| `texts`                   | 24    | Plain texts with JSONB tags; all have body content                    |
| `interlinear_texts`       | 18    | No body field — body must be assembled from sentences                 |
| `interlinear_sentences`   | 175   | Only needed for Phase 1 to assemble interlinear_texts body            |
| `text_versions`           | 49    | Discard entirely                                                       |
| `word_alignments`         | 2,836 | Phase 2 only                                                           |
| `annotations`             | 147   | Phase 2 only                                                           |
| `annotation_words`        | 69    | Phase 2 only                                                           |
| `training_sessions`       | 26    | Phase 2 only                                                           |
| `training_session_results`| 421   | Phase 2 only                                                           |
| `training_session_words`  | 324   | Phase 2 only                                                           |

**Target row counts after Phase 1:**
- `arabic_roots`: 145
- `words`: 231
- `word_dictionary_links`: 1,458
- `word_progress`: 231 (create rows for the 50 words without tracking, using defaults)
- `texts`: 42 (24 plain + 18 interlinear, no title overlap except "Tradizioni di Natale" — see step 5)
- `text_tags`: ~57 tag instances from 24 plain texts; interlinear_texts have no tags

---

### Key schema changes reference

| an-na7wi source                          | qalam target                     | Notes                                                          |
|------------------------------------------|----------------------------------|----------------------------------------------------------------|
| `arabic_roots.letters` (JSONB `["ر","ش","د"]`) | `arabic_roots.letters` (TEXT[]) | Parse JSON array → Postgres array literal                     |
| `arabic_roots.meaning`                   | `arabic_roots.meaning`           | 42 roots have a non-empty meaning; 103 have NULL               |
| `arabic_roots.analysis`                  | `arabic_roots.analysis`          | All NULL in dump — pass through as NULL                        |
| `words.arabic`                           | `words.arabic_text`              | Rename                                                         |
| `words.example`                          | `word_examples` table            | Move to child table (231 words, only 4 have content — see notes) |
| `words.pronunciation_link`               | `words.pronunciation_url`        | Rename; 209 of 231 words have a Forvo URL                      |
| `words.notes`                            | `words.translation` (unchanged)  | NOT a rename — `notes` in an-na7wi is a freetext field (64 have content); no equivalent column in qalam words; put in `word_examples` or discard |
| `words.root` (text, e.g. `'ط-ب-ع'`)     | (discard)                        | Legacy text column, superseded by `root_id` UUID FK            |
| `words.frequency`                        | (discard)                        | qalam has no frequency column                                  |
| `words.derived_from`                     | `words.derived_from_id`          | Rename; only 3 rows non-NULL — verify UUIDs exist in target    |
| `dictionary_links.type`                  | `word_dictionary_links.source`   | Rename column; same values; no CUSTOM rows in this dump        |
| `dictionary_links.display_name`          | (discard)                        | qalam `word_dictionary_links` has no display_name column       |
| `texts.arabic_content`                   | `texts.body`                     | Rename                                                         |
| `texts.tags` (JSONB string array)        | `text_tags` rows                 | Expand `["tag1","tag2"]` → one row per tag                     |
| `texts.word_count`                       | (discard)                        | No word_count in qalam texts                                   |
| `texts.is_current_version`/`current_version` | (discard)                   | Versioning removed                                             |
| `texts.parent_text_id`/`version_id`      | (discard)                        | Versioning removed                                             |
| `interlinear_texts`                      | `texts`                          | Treat as texts with no body; assemble body from sentences      |
| `interlinear_texts.description`          | `texts.comments`                 | 15 of 18 have a description                                    |
| `interlinear_texts` (no difficulty)      | `texts.difficulty`               | Default to BEGINNER — no difficulty data available             |
| `word_progress_tracking`                 | `word_progress`                  | Rename table; drop `id`, `mastery_level_updated_at`, `created_at`, `updated_at` |

**Important clarification on `words.notes`:** In an-na7wi, `notes` is a freetext field with linguistic commentary (e.g. "Feminine form: جميلة", "Very important verb..."). Qalam's `words` table has no notes/comments field. The cleanest approach is to discard it. If you want to preserve it, add a `notes TEXT` column to qalam's `words` table before migrating (out of scope for this plan but a 1-line DDL).

**`words.example` mapping:** The source `example` column contains a raw Arabic sentence string (only 4 of 231 rows are non-empty). Qalam has a proper `word_examples` child table. Migrate any non-empty examples as a single `word_examples` row with `arabic = source.example`, `transliteration = NULL`, `translation = NULL`.

---

### Migration execution order

```
1. arabic_roots          (no FKs)
2. words                 (FK: root_id → arabic_roots, derived_from_id → words)
3. word_examples         (FK: word_id → words)  [only 4 rows]
4. word_dictionary_links (FK: word_id → words)
5. word_progress         (FK: word_id → words)
6. texts                 (no FKs)
7. text_tags             (FK: text_id → texts)
```

---

### Step-by-step script outlines

All scripts connect to two DBs:
- **OLD**: the restored an-na7wi dump — referred to as `$OLD_DSN`
- **NEW**: the running qalam DB — referred to as `$NEW_DSN`

Suggested env vars:
```bash
export OLD_DSN="postgresql://localhost:5433/annahwi"
export NEW_DSN="postgresql://localhost:5432/qalam"
```

---

#### Script 1: `migrate_01_roots.py`

Migrates `arabic_roots`. Preserves original UUIDs (words reference them by root_id).

```python
# Columns read from OLD: id, letters, normalized_form, display_form, letter_count, meaning, analysis, created_at, updated_at
# letters is a JSONB string like '["ر", "ش", "د"]' — parse with json.loads(), then format as Postgres array literal
# Constraint: letter_count BETWEEN 2 AND 6 — all 145 source rows are 3 or 4; no filtering needed
# meaning: 103 rows are NULL, 42 have content. 99 have empty string '' — coerce '' to NULL.
# analysis: all NULL in dump — pass through.

import psycopg2, json, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT id, letters, normalized_form, display_form, letter_count,
               NULLIF(meaning, ''), analysis, created_at, updated_at
        FROM arabic_roots
    """)
    rows = src.fetchall()
    for (id_, letters_json, norm, disp, lc, meaning, analysis, cat, uat) in rows:
        # letters_json is a Python string like '["ر", "ش", "د"]'
        letters_list = json.loads(letters_json)  # → ["ر", "ش", "د"]
        letters_pg = '{' + ','.join(f'"{l}"' for l in letters_list) + '}'
        dst.execute("""
            INSERT INTO arabic_roots
                (id, letters, normalized_form, display_form, letter_count, meaning, analysis, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (normalized_form) DO NOTHING
        """, (id_, letters_pg, norm, disp, lc, meaning, analysis, cat, uat))
    new.commit()

print(f"Inserted {len(rows)} roots")
```

**Expected output:** 145 rows inserted.

---

#### Script 2: `migrate_02_words.py`

Migrates `words`. Preserves original UUIDs (word_progress and dictionary_links reference them).

```python
# Columns read from OLD:
#   id, arabic (→ arabic_text), transliteration, translation,
#   part_of_speech, difficulty, dialect, mastery_level,
#   pronunciation_link (→ pronunciation_url), root_id, derived_from (→ derived_from_id),
#   created_at
# Dropped: root (text column), notes, frequency, example, is_verified, related_words, updated_at
#
# UNKNOWN POS: source enum has no UNKNOWN; qalam allows it — no issue.
# Empty string notes → discard (no target column).
# mastery_level: passed through as-is (same enum values: NEW/LEARNING/KNOWN/MASTERED).
# derived_from: only 3 rows non-NULL; verify those IDs exist in the rows already inserted
#   (self-referential FK — insert parent rows first; in practice all 231 rows inserted in one
#    batch then FK set, or insert with derived_from_id = NULL and UPDATE afterward).

import psycopg2, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

with old.cursor() as src, new.cursor() as dst:
    # Phase A: insert all words with derived_from_id = NULL
    src.execute("""
        SELECT id, arabic, transliteration, translation, part_of_speech,
               difficulty, dialect, mastery_level, pronunciation_link,
               root_id, derived_from, created_at
        FROM words
    """)
    rows = src.fetchall()
    derived_pairs = []   # (child_id, parent_id) where parent_id is non-NULL

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

    # Phase B: back-fill derived_from_id for 3 rows
    for child_id, parent_id in derived_pairs:
        dst.execute(
            "UPDATE words SET derived_from_id = %s WHERE id = %s",
            (parent_id, child_id)
        )
    new.commit()

print(f"Inserted {len(rows)} words, patched {len(derived_pairs)} derived_from links")
```

**Expected output:** 231 words inserted, 3 derived_from links patched.

---

#### Script 3: `migrate_03_word_examples.py`

Migrates non-empty `words.example` values to `word_examples` rows.

```python
# Only 4 of 231 rows have a non-empty example field.
# Source: words.example (raw Arabic text, no transliteration/translation).
# Target: word_examples(id uuid, word_id, arabic, transliteration NULL, translation NULL)
# Generate a new UUID for each row (source has no example ID).

import psycopg2, os, uuid

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

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
```

**Expected output:** 4 rows.

---

#### Script 4: `migrate_04_dictionary_links.py`

Migrates `dictionary_links` (1,458 rows) to `word_dictionary_links`.

```python
# Source columns: id, word_id, type (→ source), url, display_name (dropped)
# Target: word_dictionary_links(id, word_id, source, url)
# All 7 source types (ALMANY, LIVING_ARABIC, DERJA_NINJA, REVERSO, WIKTIONARY,
#   ARABIC_STUDENT_DICTIONARY, LANGENSCHEIDT) are valid in qalam CHECK constraint.
# No CUSTOM rows present in this dump.
# Preserve original UUIDs.

import psycopg2, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

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
```

**Expected output:** 1,458 rows.

---

#### Script 5: `migrate_05_word_progress.py`

Migrates `word_progress_tracking` to `word_progress`. Creates default rows for the 50 words without tracking data.

```python
# Source: word_progress_tracking(id, word_id, consecutive_correct, total_attempts,
#          total_correct, last_reviewed_at, mastery_level_updated_at, created_at, updated_at)
# Target: word_progress(word_id PK, consecutive_correct, total_attempts, total_correct, last_reviewed_at)
# Dropped: id, mastery_level_updated_at, created_at, updated_at
# 181 rows in source; 231 words total → 50 words need a default row (all zeros, last_reviewed_at NULL).

import psycopg2, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

with old.cursor() as src, new.cursor() as dst:
    src.execute("""
        SELECT word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at
        FROM word_progress_tracking
    """)
    tracked = src.fetchall()
    tracked_ids = set(r[0] for r in tracked)

    # Insert tracked words
    dst.executemany("""
        INSERT INTO word_progress (word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (word_id) DO NOTHING
    """, tracked)

    # Insert default rows for untracked words
    dst.execute("SELECT id FROM words WHERE id NOT IN %s", (tuple(tracked_ids),))
    untracked = dst.fetchall()
    dst.executemany("""
        INSERT INTO word_progress (word_id, consecutive_correct, total_attempts, total_correct, last_reviewed_at)
        VALUES (%s, 0, 0, 0, NULL)
        ON CONFLICT (word_id) DO NOTHING
    """, [(r[0],) for r in untracked])

    new.commit()

print(f"Inserted {len(tracked)} tracked + {len(untracked)} default word_progress rows")
```

**Expected output:** 181 tracked + 50 default = 231 rows.

---

#### Script 6: `migrate_06_texts.py`

Migrates plain `texts` (24 rows) and `interlinear_texts` (18 rows) into the unified qalam `texts` table.

```python
# --- PLAIN TEXTS (24 rows) ---
# Source columns: id, title, arabic_content (→ body), transliteration, translation,
#   tags (JSONB), difficulty, dialect, comments, created_at
# Dropped: word_count, is_current_version, parent_text_id, version_id, current_version, updated_at
# Preserve original UUIDs (text_tags and, in Phase 2, sentences reference them).
#
# --- INTERLINEAR TEXTS (18 rows) ---
# Source: interlinear_texts(id, title, description(→comments), dialect, created_at)
# No body — assemble from interlinear_sentences.arabic_text ordered by sentence_order,
#   joined with newlines. Do NOT join sentences from plain texts.
# No difficulty field → default BEGINNER.
# One title overlap: "Tradizioni di Natale" exists in both tables.
#   Decision: keep BOTH records (different content/context); they will have different UUIDs.
#   The plain-text version already has body content; the interlinear version gets a body
#   assembled from sentences. Differentiate at app level by checking if sentences exist.
#
# interlinear_texts dialect: TUNISIAN (17), LEVANTINE (1) — both valid in qalam.

import psycopg2, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

with old.cursor() as src_texts, old.cursor() as src_inter, old.cursor() as src_sents, new.cursor() as dst:

    # -- Plain texts --
    src_texts.execute("""
        SELECT id, title, arabic_content, transliteration, translation,
               difficulty, dialect, comments, created_at
        FROM texts
    """)
    for (id_, title, body, translit, trans, diff, dial, comments, cat) in src_texts.fetchall():
        dst.execute("""
            INSERT INTO texts (id, title, body, transliteration, translation,
                               difficulty, dialect, comments, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (id_, title, body, translit, trans, diff, dial, comments, cat, cat))

    # -- Interlinear texts --
    src_inter.execute("""
        SELECT id, title, description, dialect, created_at
        FROM interlinear_texts
    """)
    for (id_, title, desc, dial, cat) in src_inter.fetchall():
        # Assemble body from sentences
        src_sents.execute("""
            SELECT arabic_text FROM interlinear_sentences
            WHERE text_id = %s
            ORDER BY sentence_order
        """, (id_,))
        sentences = [r[0] for r in src_sents.fetchall()]
        body = '\n'.join(sentences) if sentences else ''
        dst.execute("""
            INSERT INTO texts (id, title, body, transliteration, translation,
                               difficulty, dialect, comments, created_at, updated_at)
            VALUES (%s, %s, %s, NULL, NULL, 'BEGINNER', %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (id_, title, body, dial, desc, cat, cat))

    new.commit()

print("Texts migration complete")
```

**Expected output:** 42 rows (24 plain + 18 interlinear).

---

#### Script 7: `migrate_07_text_tags.py`

Expands the JSONB `tags` array from `texts` into `text_tags` rows.

```python
# Source: texts.tags — a JSONB column containing a JSON array of strings,
#   e.g. '["daily life", "directions", "city"]'
# 24 plain texts with tags; total ~57 tag instances.
# interlinear_texts have no tags — skip.
# Target: text_tags(text_id, tag) — composite PK, no surrogate.

import psycopg2, json, os

old = psycopg2.connect(os.environ['OLD_DSN'])
new = psycopg2.connect(os.environ['NEW_DSN'])

with old.cursor() as src, new.cursor() as dst:
    src.execute("SELECT id, tags FROM texts WHERE tags IS NOT NULL AND tags != '[]'")
    total = 0
    for (text_id, tags_json) in src.fetchall():
        if isinstance(tags_json, str):
            tags = json.loads(tags_json)
        else:
            tags = tags_json  # psycopg2 may auto-parse JSONB
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
```

**Expected output:** ~57 rows.

---

### Data quality issues found in dump

| Issue | Details | Handling |
|-------|---------|----------|
| `arabic_roots.letters` is JSONB string, not TEXT[] | Source: `'["ر", "ش", "د"]'`; qalam: TEXT[] | Parse with `json.loads()` → format as `{ر,ش,د}` |
| `arabic_roots.meaning` empty strings | 99 of 145 roots have `meaning = ''` | Coerce `''` → NULL (Script 1 uses `NULLIF`) |
| `arabic_roots.analysis` all NULL | All 145 rows have NULL analysis | Pass through; no issue |
| `words.notes` has no target column | 64 of 231 words have notes content | Discard unless you add a `notes TEXT` column to qalam words |
| `words.example` garbled content | Only 4 rows non-empty; content is mixed Arabic/transliteration raw text (e.g. `'طلق\u200e ھواء\u200e (ṭalq) open air\u200e'`) | Migrate as-is; spot-check those 4 rows after migration |
| 50 words with no `word_progress_tracking` row | All 231 words need a `word_progress` row in qalam | Script 5 creates zero-default rows for untracked words |
| `interlinear_texts` has no body or difficulty | 18 texts have no body column | Assemble body from ordered sentences (Script 6); default BEGINNER difficulty |
| 1 overlapping title between `texts` and `interlinear_texts` | "Tradizioni di Natale" appears in both | Keep both; they have different UUIDs and different content |
| `interlinear_sentences.annotations` column name misleading | Actually contains free translation (e.g. "We shall exploit this opportunity…") | Phase 2 only; maps to `sentences.free_translation`, not `sentences.notes` |
| `word_alignments` has 4 rows with `vocabulary_word_id` set | 2,832 of 2,836 have NULL vocabulary link | Phase 2 only; low linkage rate — don't rely on this field |
| `words.derived_from` only 3 rows non-NULL | Self-referential FK | Insert all rows first with NULL, then patch (Script 2) |
| Empty string POS | Source POS has no UNKNOWN; some `notes` fields say "UNKNOWN pos" — but actual column values are valid (NOUN, VERB, etc.) | No action needed |
| `dictionary_links.display_name` column exists in source, not in target | 1,458 rows have display_name values | Discard — qalam has no display_name on word_dictionary_links |

---

### Verification queries

Run these against the qalam DB after all 7 scripts complete.

```sql
-- Row count checks
SELECT 'arabic_roots' AS tbl, COUNT(*) FROM arabic_roots
UNION ALL SELECT 'words',                COUNT(*) FROM words
UNION ALL SELECT 'word_examples',        COUNT(*) FROM word_examples
UNION ALL SELECT 'word_dictionary_links',COUNT(*) FROM word_dictionary_links
UNION ALL SELECT 'word_progress',        COUNT(*) FROM word_progress
UNION ALL SELECT 'texts',                COUNT(*) FROM texts
UNION ALL SELECT 'text_tags',            COUNT(*) FROM text_tags;
-- Expected: 145 / 231 / 4 / 1458 / 231 / 42 / ~57

-- FK integrity: words → roots
SELECT COUNT(*) FROM words w
LEFT JOIN arabic_roots r ON r.id = w.root_id
WHERE w.root_id IS NOT NULL AND r.id IS NULL;
-- Expected: 0

-- FK integrity: words → words (derived_from)
SELECT COUNT(*) FROM words w
LEFT JOIN words p ON p.id = w.derived_from_id
WHERE w.derived_from_id IS NOT NULL AND p.id IS NULL;
-- Expected: 0

-- FK integrity: word_dictionary_links → words
SELECT COUNT(*) FROM word_dictionary_links wdl
LEFT JOIN words w ON w.id = wdl.word_id
WHERE w.id IS NULL;
-- Expected: 0

-- FK integrity: word_progress → words (all words covered)
SELECT COUNT(*) FROM words w
LEFT JOIN word_progress wp ON wp.word_id = w.id
WHERE wp.word_id IS NULL;
-- Expected: 0

-- FK integrity: text_tags → texts
SELECT COUNT(*) FROM text_tags tt
LEFT JOIN texts t ON t.id = tt.text_id
WHERE t.id IS NULL;
-- Expected: 0

-- Sanity: words with arabic_text empty or NULL
SELECT COUNT(*) FROM words WHERE arabic_text IS NULL OR arabic_text = '';
-- Expected: 0

-- Sanity: texts with empty body
SELECT id, title FROM texts WHERE body IS NULL OR body = '';
-- Expected: 0 (interlinear_texts with no sentences would show here — verify none)

-- Spot-check: a word with known links
SELECT w.arabic_text, w.transliteration, w.translation, r.normalized_form,
       COUNT(wdl.id) AS link_count, wp.consecutive_correct
FROM words w
LEFT JOIN arabic_roots r ON r.id = w.root_id
LEFT JOIN word_dictionary_links wdl ON wdl.word_id = w.id
LEFT JOIN word_progress wp ON wp.word_id = w.id
WHERE w.arabic_text = 'طبيعة'
GROUP BY w.arabic_text, w.transliteration, w.translation, r.normalized_form, wp.consecutive_correct;
-- Expected: arabic_text=طبيعة, normalized_form=طبع, link_count=3 (or whatever source has)

-- Spot-check: tags on a known text
SELECT t.title, array_agg(tt.tag ORDER BY tt.tag)
FROM texts t
JOIN text_tags tt ON tt.text_id = t.id
WHERE t.title = 'My city and its neighbourhoods'
GROUP BY t.title;
-- Expected: {"city","daily life","directions"}

-- Spot-check: interlinear text body assembled correctly
SELECT title, length(body), left(body, 80) FROM texts
WHERE title = 'Sentences with  شد';
-- Expected: non-empty body containing the first sentence
```

---

### Rollback approach

Scripts use `ON CONFLICT DO NOTHING` throughout, so re-running is idempotent after a failed run.

For a clean rollback before trying again:

```sql
-- Run in order (reverse FK dependency)
TRUNCATE text_tags CASCADE;
TRUNCATE texts CASCADE;
TRUNCATE word_progress CASCADE;
TRUNCATE word_dictionary_links CASCADE;
TRUNCATE word_examples CASCADE;
TRUNCATE words CASCADE;
TRUNCATE arabic_roots CASCADE;
```

Or, if qalam DB has no data yet and you want a full reset:
```bash
psql $NEW_DSN -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
# Then re-run Flyway: just backend (Flyway runs on startup)
```

The source an-na7wi DB is never written to by these scripts — it is read-only throughout.

---

### Key schema changes reference (complete)

| an-na7wi | qalam | Notes |
|----------|-------|-------|
| `words.arabic` | `arabic_text` | Rename |
| `words.example` | `word_examples.arabic` | Move to child table |
| `words.pronunciation_link` | `pronunciation_url` | Rename |
| `words.notes` | (no mapping) | No target column — discard or add column manually |
| `words.root` (text) | (discard) | Superseded by root_id FK |
| `words.frequency` | (discard) | No frequency in qalam |
| `words.derived_from` | `derived_from_id` | Rename |
| `dictionary_links.type` | `word_dictionary_links.source` | Rename column |
| `dictionary_links.display_name` | (discard) | No target column |
| `texts.arabic_content` | `body` | Rename |
| `texts.tags` (JSONB array) | `text_tags` rows | Expand array |
| `texts` + `interlinear_texts` | unified `texts` | Merge; assemble body for interlinear |
| `interlinear_texts.description` | `texts.comments` | Rename |
| `text_versions` | (discard) | Versioning removed |
| `interlinear_sentences.translation` | `sentences.free_translation` | Phase 2; rename (bug fix) |
| `interlinear_sentences.annotations` | `sentences.free_translation` | Phase 2; misleading name — it IS free_translation |
| `word_alignments` | `alignment_tokens` | Phase 2; rename table |
| `word_progress_tracking` | `word_progress` | Rename table |
| `word_progress_tracking.mastery_level_updated_at` | (discard) | No equivalent column |
