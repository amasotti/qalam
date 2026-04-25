---
description: Full tokenization pipeline for a qalam text — splits into sentences, POSTs each, then auto-tokenizes or falls back to manual interlinear glossing
argument-hint: "<text-id>"
---

If `$ARGUMENTS` is empty, stop and say: this command requires a text ID (e.g. `/tokenize a1b2c3d4-...`). Do not proceed.

Otherwise run the full pipeline below for text ID `$ARGUMENTS`. API base: `http://localhost:8085`. No auth needed.

---

## Step 1 — Fetch the text

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS
```

Extract `body` (raw Arabic). If `sentences` array is **non-empty**, stop and ask the user — the text may already be partially processed.

## Step 2 — Split into sentences

Split `body` on sentence-final punctuation: `.` `؟` `!` and paragraph breaks (`\n\n`).

Rules:
- `،` (Arabic comma) does **not** end a sentence — it marks a pause clause. Do not split on it.
- Trim whitespace. Skip empty strings.
- Preserve order.

## Step 3 — POST each sentence

```
POST /api/v1/texts/$ARGUMENTS/sentences
Content-Type: application/json

{
  "arabicText": "<sentence>",
  "position": 0,
  "transliteration": "<full sentence transliteration — see conventions below>",
  "freeTranslation": "<English translation of the full sentence>",
  "notes": "<optional: grammatical structures worth flagging>"
}
```

Record the returned `id` for each sentence. Process in order. Stop on any error — do not continue to the next sentence.

## Step 4 — Tokenize each sentence

**Try auto-tokenize first:**

```
POST /api/v1/texts/$ARGUMENTS/sentences/<sentenceId>/auto-tokenize
```

- `200`: done for this sentence, move on.
- `503` (AI not configured): fall through to manual tokenization.

**Manual tokenization fallback:**

Split the sentence into individual word tokens. For each token:

| Field | Content |
|---|---|
| `position` | 0-based, left-to-right |
| `arabic` | Word with diacritics (add diacritics based on linguistic analysis) |
| `transliteration` | Arabi chat alphabet, dialect-adjusted (see conventions below) |
| `translation` | Minimal word-level gloss. Include grammatical hint in parens when useful: `"was (f.)"` |

```
PUT /api/v1/texts/$ARGUMENTS/sentences/<sentenceId>/tokens
Content-Type: application/json

{
  "tokens": [
    { "position": 0, "arabic": "كَانَتْ", "transliteration": "kānat", "translation": "was (f.)" },
    ...
  ]
}
```

PUT replaces ALL tokens atomically. Positions must be contiguous from 0.

## Step 5 — Verify

```
GET /api/v1/texts/$ARGUMENTS
```

Confirm `sentences` array length matches your split. Spot-check one sentence's tokens.

---

## Transliteration conventions

Use Arabi chat alphabet, close to dialect pronunciation. Default dialect: **Tunisian** unless stated otherwise.

- `9` for قاف (`قهوة` → `9ahwa`)
- `5` for خاء
- `3` for عين
- `7` for حاء
- Use the rule-based endpoint as a starting point, then correct for vowels and dialect:

```bash
curl -s -X POST http://localhost:8085/api/v1/transliterate \
  -H "Content-Type: application/json" \
  -d '{"arabic": "<word or phrase>"}'
```

If the conjunction `و` is attached orthographically, keep it attached in the transliteration too.

---

## Error handling

| Situation | Action |
|---|---|
| `GET /texts/<id>` → 404 | Stop. Report ID not found. |
| `sentences` already non-empty | Stop. Ask user before proceeding. |
| `POST /sentences` → error | Stop. Report error. Do not continue. |
| `auto-tokenize` → 503 | Fall back to manual. Do not report as error. |
| `PUT /tokens` → 422 | Check positions are contiguous from 0. Fix and retry. |