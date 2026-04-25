---
description: Auto-generate linguistic annotations (vocabulary, grammar, cultural) for a tokenized qalam text and POST them via the API
argument-hint: "<text-id>"
---

If `$ARGUMENTS` is empty, stop and say: pass a text ID (e.g. `/annotate a1b2c3d4-...`).

API base: `http://localhost:8085`. No auth needed.

---

## Step 1 — Fetch the text and sentences

```bash
curl -s http://localhost:8085/api/v1/texts/$ARGUMENTS
```

If the response is 404, stop and report.

Check `sentences`. If empty, stop and say: the text has no sentences yet — run `/tokenize $ARGUMENTS` first.

## Step 2 — Check existing annotations

```bash
curl -s "http://localhost:8085/api/v1/texts/$ARGUMENTS/annotations"
```

If annotations already exist, list how many and ask: "There are already N annotations. Add more? (yes / no)"

## Step 3 — Analyze and generate annotations

Read each sentence's `arabicText`, `tokens`, `freeTranslation`, and `notes`. Generate 3–10 high-value annotations for the text. Prioritize:

**Vocabulary** (`type: VOCAB`) — for words that:
- Are root-transparent and worth noting (Form II/V/VIII verbs, broken plurals, مصدر forms)
- Have semantic range beyond the obvious gloss
- Recur across sentences

**Grammar** (`type: GRAMMAR`) — for constructions that:
- Are non-trivial (iḍāfa chains, kāna clauses, conditional structures, relative clauses)
- Are worth active recall: name the pattern, explain what makes it interesting

**Cultural** (`type: CULTURAL`) — for allusions, proverbs, register markers, or references a reader without background would miss.

**Structural** (`type: STRUCTURAL`) — for discourse-level observations: paragraph structure, argument flow, rhyme/rhythm in prose.

Prefer anchors that are exact Arabic substrings of the text body (so the frontend can highlight them). Keep `anchor` short — a word or short phrase, not a full sentence.

## Step 4 — Preview and confirm

List all planned annotations in a table:

| anchor | type | content (truncated) |
|--------|------|---------------------|
| مضطربة | VOCAB | Root ط-ر-ب Form VIII... |

Ask: "POST these N annotations? (yes / no / edit N)"

## Step 5 — POST each annotation

```
POST /api/v1/texts/$ARGUMENTS/annotations
Content-Type: application/json

{
  "anchor": "<exact Arabic substring>",
  "type": "VOCAB|GRAMMAR|CULTURAL|STRUCTURAL",
  "content": "<the note>",
  "masteryLevel": "LEARNING",
  "reviewFlag": false
}
```

Report each created annotation's ID. Stop on any error.

## Step 6 — Summary

Print: N annotations created, any skipped/errored.
