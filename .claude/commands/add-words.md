---
description: Bulk-create vocabulary word entries from a comma-separated Arabic word list, with AI-filled metadata (transliteration, translation, POS, difficulty)
argument-hint: "<word1,word2,word3,...>"
---

If `$ARGUMENTS` is empty, stop and say: pass a comma-separated list of Arabic words (e.g. `/add-words كتب,قرأ,بيت`).

API base: `http://localhost:8085`. No auth needed.

---

Process each word in `$ARGUMENTS` (split on `,`, trim whitespace) in order.

## For each word

### 1. Check if it already exists

```bash
curl -s "http://localhost:8085/api/v1/words/autocomplete?q=<word>&limit=3"
```

If an exact match on `arabicText` is found, skip creation and note it as "already exists". Continue to the next word.

### 2. Determine metadata

Using your Arabic linguistic knowledge, determine:

- **`transliteration`** — Arabi chat alphabet. Start from the rule-based endpoint:
  ```bash
  curl -s -X POST http://localhost:8085/api/v1/transliterate \
    -H "Content-Type: application/json" \
    -d '{"arabic": "<word>"}'
  ```
  Then correct for vowel length and standard pronunciation.

- **`translation`** — concise English gloss (2–5 words). For verbs: infinitive form. For nouns: include gender hint if clear, e.g. `"house (m.)"`.

- **`partOfSpeech`** — one of: `NOUN`, `VERB`, `ADJECTIVE`, `ADVERB`, `PREPOSITION`, `CONJUNCTION`, `PARTICLE`, `PRONOUN`, `INTERJECTION`, `OTHER`

- **`dialect`** — default `MSA` unless the word is clearly dialectal.

- **`difficulty`** — one of: `BEGINNER`, `ELEMENTARY`, `INTERMEDIATE`, `UPPER_INTERMEDIATE`, `ADVANCED`, `MASTERY`

- **`rootId`** — leave absent (the root can be linked later via the UI).

### 3. POST the word

```
POST /api/v1/words
Content-Type: application/json

{
  "arabicText": "<word>",
  "transliteration": "<transliteration>",
  "translation": "<gloss>",
  "partOfSpeech": "<POS>",
  "dialect": "MSA",
  "difficulty": "<difficulty>"
}
```

Record the returned `id`.

---

## After all words

Print a summary table:

| Word | Status | ID |
|------|--------|----|
| كتب | created | uuid... |
| قرأ | already existed | — |

Report any errors with the HTTP status and response body.
