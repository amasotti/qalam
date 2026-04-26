---
description: Sentence-split pipeline for a qalam text — splits body into sentences, POSTs each with transliteration and free translation
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

Process in order. Stop on any error — do not continue to the next sentence.

## Step 4 — Verify

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS/sentences
```

Confirm array length matches your split count.

---

## Transliteration conventions

Use Arabi chat alphabet, close to dialect pronunciation. Default dialect: **Tunisian** unless stated otherwise.

- `9` for قاف
- `5` for خاء
- `3` for عين
- `7` for حاء
- Emphatic (dark) consonants use **capital** Latin letters: `ط = T`, `ص = S`, `ض = D`, `ظ = D`
- Use the rule-based endpoint as a starting point, then correct for vowels and dialect:

```bash
curl -s -X POST http://localhost:8085/api/v1/transliterate \
  -H "Content-Type: application/json" \
  -d '{"arabic": "<word or phrase>"}'
```

If the conjunction `و` is attached orthographically, keep it attached in the transliteration too.
3ayn is always rendered as `3`. So `I do` is `na3mel`, never `namel`.

---

## Error handling

| Situation | Action |
|---|---|
| `GET /texts/<id>` → 404 | Stop. Report ID not found. |
| `GET /sentences` → non-empty array | Stop. Ask user before proceeding. |
| `POST /sentences` → 500 | Check `arabicText` does not contain trailing punctuation (`.` `؟` `!`). Strip and retry. |
| `POST /sentences` → other error | Stop. Report error. Do not continue. |
