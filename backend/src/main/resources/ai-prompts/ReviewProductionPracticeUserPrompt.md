# Inputs

Learner sentence:
[<sentence>]

Learner's intended meaning (optional; may be in any language):
[<intendedMeaning>]

Target vocabulary assigned:
[<targetWords>]

Words the learner intended/attempted to use:
[<usedWords>]

# Review task

If an intended meaning is supplied, first identify what the Arabic actually says, then compare it with the intended
meaning. State whether they match, partly match, or differ, and name the precise gap. A grammatical sentence with
unintended meaning is a semantic mismatch, not a grammar error. If no intended meaning is supplied, assess only the
meaning expressed by the Arabic.

Review the selected target words that the learner actually attempted. Do not invent a problem for every selected word.

# Output structure

Respond with these level-two Markdown headings in exactly this order:

## Meaning

State what the Arabic sentence communicates in the feedback language. When an intended meaning was supplied, compare it
directly with that meaning.

## Error breakdown

List only real issues, one bullet per issue: original fragment → correction — concise reason. Separate grammatical
issues from semantic mismatches. If there are none, say so in one sentence.

## Target vocabulary

Discuss the selected target words actually attempted: form, meaning, fit, and any needed preposition or collocation.

## Correct sentence

Provide one closest, minimal, natural sentence expressing the intended meaning, or the meaning actually expressed when
none was supplied. Include Arabic, Latin transliteration, and a translation in the feedback language. If no correction
is needed, say so in the feedback language.

## Alternatives

Offer 3-5 concise, high-value ways to express the same intended proposition when they expand the learner's
usable repertoire. Vary wording, causal or agentive framing, verb choice, preposition, construction, or register;
preserve the core intended idea and explain any changed nuance. Each alternative must include Arabic, Latin
transliteration, a literal translation in the feedback language, and one short usage note. Never repeat the correct
sentence or offer near-identical substitutions. If no alternative adds real learning value, say so in the feedback
language.
