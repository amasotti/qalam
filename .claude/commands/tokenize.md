---
description: Full tokenization pipeline for a qalam text — splits into sentences, POSTs each, auto-tokenizes or splits manually, then adds word-level glosses to every token
argument-hint: "<text-id>"
---

If `$ARGUMENTS` is empty, stop and say: this command requires a text ID (e.g. `/tokenize a1b2c3d4-...`). Do not proceed.

Otherwise run the full pipeline below for text ID `$ARGUMENTS`. API base: `http://localhost:8085`. No auth needed.

---

## Step 1 — Fetch the text

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS
```

Extract `body` (raw Arabic). Check for existing sentences via:

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS/sentences
```

If the returned array is **non-empty**, stop and ask the user — the text may already be partially processed.

## Step 2 — Split into sentences

Split `body` on sentence-final punctuation: `.` `؟` `!` and paragraph breaks (`\n\n`).

Rules:
- `،` (Arabic comma) does **not** end a sentence — it marks a pause clause. Do not split on it.
- Strip the sentence-final punctuation character from `arabicText` — do not include it in the POST body.
- Trim whitespace. Skip empty strings.
- Preserve order.

## Step 3 — POST each sentence

```
POST /api/v1/texts/$ARGUMENTS/sentences
Content-Type: application/json

{
  "arabicText": "<sentence without trailing punctuation>",
  "position": 0,
  "transliteration": "<full sentence transliteration — see conventions below>",
  "freeTranslation": "<English translation of the full sentence>",
  "notes": "<optional: grammatical structures worth flagging>"
}
```

Record the returned `id` for each sentence. Process in order. Stop on any error — do not continue to the next sentence.

## Step 4 — Split into tokens

**Try auto-tokenize first:**

```
POST /api/v1/texts/$ARGUMENTS/sentences/<sentenceId>/auto-tokenize
```

- `200`: tokens created (arabic text only, no glosses yet) — continue to Step 5.
- `503` (AI not configured): fall back to manual split below.

**Manual split fallback:**

Split the sentence into individual word tokens and PUT them with arabic text only (glosses come in Step 5):

```
PUT /api/v1/texts/$ARGUMENTS/sentences/<sentenceId>/tokens
Content-Type: application/json

{
  "tokens": [
    { "position": 0, "arabic": "كَانَتْ", "transliteration": null, "translation": null },
    ...
  ]
}
```

PUT replaces ALL tokens atomically. Positions must be contiguous from 0.

## Step 5 — Word-level glosses (always required)

**This step is mandatory regardless of whether Step 4 used auto-tokenize or manual split.**
Auto-tokenize only splits words — it never fills in transliteration or translation. You must gloss every token.

Fetch the current tokens for the sentence:

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS/sentences/<sentenceId>
```

Then PUT the full token list with transliteration and translation filled in for every token:

| Field | Content |
|---|---|
| `position` | 0-based, left-to-right |
| `arabic` | Keep as returned (do not change) |
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

Process all sentences before moving to verification. For efficiency, batch all 13 (or however many) sentences in a single Python/shell script rather than one curl per token.

## Step 6 — Verify

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS/sentences
```

Confirm array length matches your split. Spot-check one sentence's tokens — verify `transliteration` and `translation` are non-null.

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
| `GET /sentences` → non-empty array | Stop. Ask user before proceeding. |
| `POST /sentences` → 500 | Check `arabicText` does not contain trailing punctuation (`.` `؟` `!`). Strip and retry. |
| `POST /sentences` → other error | Stop. Report error. Do not continue. |
| `auto-tokenize` → 503 | Fall back to manual split. Do not report as error. |
| `PUT /tokens` → 422 | Check positions are contiguous from 0. Fix and retry. |