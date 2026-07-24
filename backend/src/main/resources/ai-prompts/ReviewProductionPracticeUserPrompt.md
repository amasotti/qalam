# Inputs

Learner sentence:
[<sentence>]

Learner's intended meaning (optional; may be in any language):
[<intendedMeaning>]

Target vocabulary assigned:
[<targetWords>]

Words the learner intended/attempted to use:
[<usedWords>]

# How to assess the attempt

1. Inspect grammar, spelling, agreement, verb form, prepositions, collocations, and clarity.
2. The learner may provide an optional intended meaning. When present, use it as the semantic target:
    - first state what the Arabic actually communicates;
    - then say whether it matches the intended meaning, partly matches it, or expresses a different meaning;
    - a grammatical sentence with the wrong meaning is a semantic mismatch, not a grammar error. When absent, assess
      only the meaning expressed by the Arabic.
3. MSA, Tunisian Arabic, and natural code-switching are all valid. Never mark mixing MSA and dialect as an error or ask
   the learner to choose one. Mention a pure-register alternative only when it teaches a meaningful difference.
4. Review only vocabulary the learner selected as intended. Recognize valid inflected forms; do not claim mechanical
   proof from a surface form alone.

Only make claims you are at least 80% confident about.

# Output rules

Output only clean Markdown, with these level-two headings in exactly this order:

## Meaning and clarity

Give a direct translation of what the Arabic says. If an intended meaning was supplied, compare it explicitly with that
translation and name the precise semantic gap, if any.

## Corrections

List only real corrections, one bullet per issue: original fragment → corrected fragment, then a brief reason. Include
semantic mismatches here only when an intended meaning was supplied. If none are needed, say so in one sentence.

## Target vocabulary

Discuss the selected target words actually attempted: form, meaning, fit, and any needed preposition or collocation. Do
not invent a problem for every word.

## Best corrected sentence

Provide one closest, minimal repair only when the attempt needs a correction. Include Arabic, Latin transliteration, and
English translation. Do not automatically provide both MSA and Derja versions. If no correction is needed, write "No
corrected sentence needed."

## Useful alternatives

Offer zero to two alternatives only when each teaches something materially different: a distinct idiom, a deliberate
register choice, a different emphasis, or a more natural construction. Never repeat the best corrected sentence, its
fully vocalized form, or a near-identical word substitution. If none add learning value, write "No additional
alternative is needed."

## Next focus

In one or two sentences, name the single most useful pattern to practise next. Ground it in this attempt.
