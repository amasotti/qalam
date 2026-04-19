---
name: tokenize-text
description:
  Use when asked to tokenize, segment, or gloss an Arabic text in the qalam app. Takes a text ID, fetches the text from localhost:8085, splits it into sentences, creates each sentence via API, then generates per-word tokens (Arabic with diacritics, transliteration, English translation) and PUTs them. Default dialect: Tunisian. Try auto-tokenize first; fall back to manual if 503.
---

# tokenize-text

## Overview

Full pipeline: fetch text → split sentences → POST sentences → tokenize each → PUT tokens.
API Server runs at `http://localhost:8085`. Default dialect/pronunciation: **Tunisian** unless caller specifies
otherwise.

## Transliteration convention

Use the Arabi chat alphabet and stay close to the dialect pronunciation. E.g.: `قهوة` is translitterated as `9ahwa`.
Use `9` for qaf, 5 for `kh`, etc...

The app has a rule-based endpoint for mechanical transliteration:

```
POST /api/v1/transliterate
{ "arabic": "<string without diacritics>" }
```

Use it as a starting point, then correct for vowel length and dialect pronunciation.

The transliteration is as close as possible to the arabic script for word separation: e.g. if the
conjunction `w` is attached to the words, so should also be the transliteration.

## Step-by-step process

### 1. Fetch the text

```bash
curl -s http://localhost:8085/api/v1/texts/<textId>
```

Extract `body` (the raw Arabic). If `sentences` array is non-empty, **stop and ask the user** — the text may already be
partially processed.

### 2. Split into sentences

Split `body` on Arabic sentence-final punctuation: `،` (Arabic comma — mid-sentence pause, keep with preceding clause),
`.`, `؟`, `!`, `\n\n`, `,`,
`.`, `;` etc...

Rules:

- `،` alone does not end a sentence — it marks a pause clause. Split only on full stop equivalents (`.`, `؟`, `!`) or
  paragraph breaks.
- Trim whitespace from each sentence. Skip empty strings.
- Preserve sentence order.

### 3. POST each sentence

```
POST /api/v1/texts/<textId>/sentences
Content-Type: application/json

{
  "arabicText": "<sentence>",
  "position": 0, // int, starting from 0 for the first sentence
  "transliteration": "<the transliteration as discussed above for the entire sentence>",
  "freeTranslation": "<your English translation of the full sentence>",
  "notes": "<optional: note grammatical structures worth flagging>"
}
```

Record the returned `id` for each sentence. Process sentences in order.

### 4. Tokenize each sentence

**Try auto-tokenize first:**

```
POST /api/v1/texts/<textId>/sentences/<sentenceId>/auto-tokenize
```

- If `200`: done for this sentence — move to next.
- If `503` (AI not configured): fall through to manual tokenization below.

**Manual tokenization fallback:**

Split sentence into individual word tokens. For each token:

| Field             | Content                                                                                                                                    |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `position`        | 1-based, left-to-right Arabic word order                                                                                                   |
| `arabic`          | Word with diacritics (shadda, tanwin, sukun where helpful for a learner). Add diacritics based on your linguistic analysis.                |
| `transliteration` | ALA-LC transliteration. For Tunisian: reflect dialect pronunciation (e.g. `q` → `g` or `ʾ`, short vowel reduction).                        |
| `translation`     | Minimal English gloss — word-level, not full sentence. Include grammatical role hint in parens when useful: `"was (f.)"`, `"life (nom.)"`. |

Particles and clitics that are orthographically attached (prefixes like `وَ`, `بِ`, `لِ`, conjunctions fused to the next
word) may be split as separate tokens or kept together — prefer splitting if the student benefit is clear.

```
PUT /api/v1/texts/<textId>/sentences/<sentenceId>/tokens
Content-Type: application/json

{
  "tokens": [
    { "position": 0, "arabic": "كَانَتْ", "transliteration": "kānat", "translation": "was (f.)" },
    ...
  ]
}
```

PUT replaces ALL tokens atomically. Positions must be contiguous from 0.
The transliteration follows the rules in this skill, the translation is actually a glossa as
you would find it in a linguistic corpus. E.g: `n7ebb` will have glossa `love-I`.

### 5. Verify

After all sentences and tokens are PUT, fetch the text again:

```
GET /api/v1/texts/<textId>
```

Confirm `sentences` array length matches your split, and spot-check one sentence's tokens.

## Dialect notes (Tunisian default)

When caller says "Tunisian":

- `ā` often shortens or centralises to 'e' — note in transliteration if significant
- Dual and sound plural forms may differ; flag in notes if text uses CA forms

For MSA/CA text with Tunisian *pronunciation* guidance: keep MSA Arabic orthography, adapt transliteration to Tunisian
phonology.

## Error handling

| Situation                     | Action                                                |
|-------------------------------|-------------------------------------------------------|
| `GET /texts/<id>` → 404       | Stop. Report ID not found.                            |
| `sentences` already non-empty | Stop. Ask user before proceeding (avoid duplicates).  |
| `POST /sentences` → error     | Stop. Report error. Do not continue to next sentence. |
| `auto-tokenize` → 503         | Fall back to manual — do not report as error.         |
| `PUT /tokens` → 422           | Check positions are contiguous from 1. Fix and retry. |

## Quick reference — endpoints used

```
GET    /api/v1/texts/<textId>
POST   /api/v1/texts/<textId>/sentences
POST   /api/v1/texts/<textId>/sentences/<sentenceId>/auto-tokenize
PUT    /api/v1/texts/<textId>/sentences/<sentenceId>/tokens
POST   /api/v1/transliterate
GET    /api/v1/texts/<textId>   (verification)
```

All requests to `http://localhost:8085`. No auth needed (single-user app).
