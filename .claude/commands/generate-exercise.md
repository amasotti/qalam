---
description: Generate a self-contained Arabic exercise markdown file from a Qalam text — fetches text via API, produces 10 mixed exercises ready for any AI model to evaluate
argument-hint: "<text-id>"
---

If `$ARGUMENTS` is empty, stop and say: this command requires a text ID (e.g. `/generate-exercise a1b2c3d4-...`). Do not proceed.

Otherwise run the pipeline below for text ID `$ARGUMENTS`. API base: `http://localhost:8085`. No auth needed.

---

## Step 1 — Fetch the text

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS
```

Extract: `title`, `dialect`, and the `sentences` array (each with `arabicText`, `freeTranslation`, `position`).

If 404 → stop, report ID not found.
If `sentences` is empty → stop, tell the user the text has no sentences yet (run `/tokenize` first).

Sort sentences by `position` ascending.

## Step 2 — Fetch vocabulary for the text

```bash
curl -s "http://localhost:8085/api/v1/texts/$ARGUMENTS/words"
```

Extract the word list: `arabic`, `translation`, `partOfSpeech`. Use these to generate vocabulary exercises.
If this endpoint returns 404 or empty, skip vocabulary exercises and generate more comprehension/grammar ones instead.

## Step 3 — Generate 10 mixed exercises

Design 5-10 exercises using the text. Mix the following types — use each type at least once:

| Type | Description |
|---|---|
| `vocabulary-mcq` | Multiple choice: meaning of a specific word in context |
| `fill-blank` | Complete a sentence from the text with a missing word |
| `true-false` | Statement about the text — true or false |
| `comprehension` | Open question answered in English |
| `translation` | Translate a short Arabic phrase or sentence to English |
| `arabic-production` | Translate a short English phrase to Arabic |

Rules:
- Ground every exercise in the actual text — no invented content
- For `vocabulary-mcq`: provide 3–4 plausible options; only one correct
- For `fill-blank`: use exact Arabic from the text; omit one word; show position in sentence
- Vary difficulty: include 2–3 harder exercises (production, nuanced meaning)
- Number exercises e.g. 1–10

## Step 4 — Write the markdown file

Output filename: `exercise-<text-id-first-8-chars>-<YYYY-MM-DD>.md` in the current working directory.

File structure:

```
# Exercise: <title>
<!-- source: qalam text <full-text-id>, generated <YYYY-MM-DD>, dialect: <dialect> -->

<!-- Complete arabic text and free translation for reference -->

## Source Text

| # | Arabic | Translation |
|---|--------|-------------|
| 1 | <arabicText> | <freeTranslation> |
| 2 | ... | ... |

---

## Exercises

### 1. [Type label] — [short description]

[Full exercise text, self-contained — no "see above" references]

[For MCQ:]
- [ ] A. option
- [ ] B. option
- [ ] C. option

[For open questions:]
> [your answer here]

[For fill-blank:]
Complete: "word1 _____ word3"  (sentence N)
> [your answer here]

[For true/false:]
- [ ] True
- [ ] False

### 2. ...

... (repeat for all 10)

---

## How to evaluate

Copy this entire file (from the top) and add the following prompt at the end, then paste into any AI model (Claude, Gemini, ChatGPT, etc.):

---

*Evaluation prompt (append to file when submitting):*

```
You are an experienced Arabic language tutor specialising in [dialect] and MSA.
The student has completed the exercises above.

For each exercise (1–10):
- State whether the answer is correct, partially correct, or incorrect
- For wrong answers: give the correct answer and a brief explanation
- Note any interesting linguistic points worth remembering

Tone: concise, academic, no motivational filler. Student level: A2–B1.
Do not restate the question — just number your feedback to match.
```
```

After writing the file, print the absolute path and a one-line summary of exercise types used.
