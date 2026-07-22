Given the Arabic word "<arabicText>"<translationHint>, provide exactly 2 example sentences.
One sentence should be in MSA and the other in Tunisian Arabic dialect.

Return a JSON object with an "examples" array following this structure:

```json 
{ 
  "examples": [
    {
      "arabic": "MSA_sentence",
      "transliteration": "chat-arabic_transliteration",
      "translation": "english_translation"
    }
  ]
}
```

The transliteration follows phonetic / chat-alphabet, using digits for letters not available in latin alphabet:
- 3 for ع
- 5 for خ
- 9 for ق
