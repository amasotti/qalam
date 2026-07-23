You are helping me creating a detailed page about the following word: <arabicText><transliterationClause> (<translation>, partOfSpeech: <partOfSpeech>, dialect: <dialect>)
Analyze it carefully, do not invent plausible-like information, just assert what you are confident enough about.
The provided part of speech is authoritative. If it is not VERB, `verbDetails` must be the literal JSON value `null`, even when the word derives from a verb.

Provide:
- gender: if the word is a Noun, then (MASCULINE/FEMININE), else null
- verbDetails: (verbs only, null for others): 
  - verbForm (Roman numeral I-X), 
  - pastPattern and presentPattern (Form I only: fa3ala/fa3ila/fa3ula and yaf3ulu/yaf3ilu/yaf3alu). Those given here are the only possible. Do not output anything other than resp. fa3ala/fa3ila/fa3ula and yaf3ulu/yaf3ilu/yaf3alu 
  - weaknessType (SOUND/ASSIMILATED/HOLLOW/GEMINATE/DEFECTIVE/DOUBLY_WEAK)
- plurals: list of plural forms with types (SOUND_MASC/SOUND_FEM/BROKEN/PAUCAL/COLLECTIVE/OTHER)
- relations: up to 5 high-value entries — synonyms with register/nuance difference, strong antonyms, or words from the same semantic field. Avoid generic filler. Each entry: arabicText (fully vocalized), transliteration (practical chat-style), translation (concise English gloss), relationType (SYNONYM/ANTONYM/RELATED)
- notes: brief mnemonic or usage note in English, focusing on common learner confusions, collocations, or register constraints (null if nothing genuinely useful)

Respond ONLY with this JSON structure (verbDetails and plurals are ofc mutually exclusive):
```json
{
  "gender": "MASCULINE | FEMININE | null",
  "verbDetails": {
    "verbForm": "I", 
    "pastPattern": "fa3ala", 
    "presentPattern": "yaf3ulu", 
    "weaknessType": "GEMINATE"
  },
  "plurals": [
    {"pluralForm": "ايام", 
      "pluralType": "BROKEN"
    }
  ],
  "relations": [
    {"arabicText": "...", 
      "transliteration": "...", 
      "translation": "...", 
      "relationType": "SYNONYM | ANTONYM | RELATED"
    }
  ],
  "notes": "..."
}
```

