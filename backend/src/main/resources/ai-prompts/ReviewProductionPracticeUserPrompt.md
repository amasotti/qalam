# Context Inputs
Learner sentence:
<sentence>

Target vocabulary words assigned:
<targetWords>

Words the learner intended/attempted to use:
<usedWords>

# Guidelines for Evaluation
1. **Dialect Awareness:** Identify whether the sentence is written in MSA, Tunisian Arabic, or a mix. Do not penalize valid Tunisian grammar/vocabulary unless it conflicts with the intended register.
2. **Depth over Surface Fixes:** Focus on B1-level grammatical accuracy (e.g., verb forms, agreement, prepositions) and natural phrasing.
3. **Concise & Direct:** Avoid conversational fluff, praise, or meta-commentary. Output ONLY the requested review.

---

# Output Format
Respond using ONLY the following level-two markdown headings in exact order:

## What I understood
* Provide a direct English translation of the learner's sentence.
* State clearly whether the core intended meaning is communicated effectively.

## Target words
* Analyze how the target words were used.
* Comment on register, correct morphological form (Form/Wazn), and naturalness. Note any misuse or missing prepositions.

## Corrections and a natural version
* Detail essential grammatical, syntactic, or morphological corrections.
* Provide a complete, fully vocalized/natural rewrite in the primary style used (MSA or Tunisian).
* If the original sentence is already correct and natural, explicitly state that no rewrite is needed.

## Alternatives
* Offer 1-2 high-value alternative structures, idioms, or vocabulary choices.
* If the learner wrote in Tunisian, offer a natural MSA equivalent (and vice versa) to build register flexibility.
* Omit weak, overly literal, or forced alternatives.
For every sentence you generate, also give a transliteration in latin alphabet and a short comment on why the suggestion is better.

## Overall feedback
* Write a substantive 3-to-5 sentence summary evaluating clarity, naturalness, and structural control.
* Identify the single most important action item for the learner's next attempt.
