Suggest between 3 and 10 NEW words in Arabic that belong in this list:
List title: <title><description><existingWords>

Each suggestion must fit the theme and must not duplicate existing words. Prefer
common, used and useful words. For each of them provide this structured output (observe it strictly):

- arabicText: the word in arabic fully vocalized with harakat
- transliteration: practical chat-alphabet style (with digits, e.g. ع is rendered as 3, خ as 5, ق as 9 etc.)
- translation: concise English gloss
- partOfSpeech: one of ${PartOfSpeech.entries.map { it.name }.joinToString()}
- difficulty: one of ${Difficulty.entries.map { it.name }.joinToString()}
- dialect: one of ${Dialect.entries.map { it.name }.joinToString()}

Prefer MSA (standard arabic) as "dialect" for most of them, but if there are interesting words in other dialects
include as well. The user is very curious about arabic culture and its dialects.

Respond ONLY AND ALWAYS with this JSON structure, no comments, no exceptions NEVER:
{
  "suggestions": [
    {"arabicText": "...", "transliteration": "...", "translation": "...", "partOfSpeech": "NOUN", "difficulty": "BEGINNER", "dialect": "MSA"}
  ]
}
