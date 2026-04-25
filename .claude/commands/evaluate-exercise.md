---
description: Send a completed exercise markdown file to Claude for evaluation — convenience wrapper around the evaluation prompt embedded in the file
argument-hint: "<path-to-exercise.md>"
---

If `$ARGUMENTS` is empty, stop and say: this command requires a path to an exercise file (e.g. `/evaluate-exercise ./exercise-a1b2c3d4-2026-04-25.md`). Do not proceed.

Read the file at `$ARGUMENTS`.

If the file does not exist or is not readable → stop, report path not found.

---

## Step 1 — Check completion

Scan the exercises. Count how many answer fields are still blank (empty `> [your answer here]`, unchecked `[ ]` with no `[x]`, or empty fill-blank lines).

If more than 3 answers are blank → warn the user:
> "X exercises have no answer. Evaluate anyway? (y/n)"
Wait for confirmation before proceeding.

## Step 2 — Evaluate

Read the full file content. Apply the evaluation prompt embedded at the bottom of the file (the block under "Evaluation prompt").

Evaluate each exercise in order:
- Correct / partially correct / incorrect
- For wrong answers: correct answer + brief explanation
- Note any interesting linguistic point if genuinely worth it
- Do not restate the question — number feedback to match exercise numbers

Tone: concise, academic, no motivational filler. Student level: A2–B1.

## Step 3 — Output

Print feedback to the terminal. Do not modify the exercise file.

Format:

```
## Evaluation — <filename>

**1.** [correct / incorrect / partial] — [feedback]
**2.** ...
...

---
Score: X/10 (approximately — open questions graded holistically)
```

After printing, offer: "Want me to append this evaluation to the file? (y/n)"
If yes, append a `## Evaluation — <date>` section to the end of the file with the same content.