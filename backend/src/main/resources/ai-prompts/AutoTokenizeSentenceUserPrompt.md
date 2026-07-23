Split this Arabic sentence into its orthographic word tokens.

IMPORTANT (never violate them)!:

- Keep clitics attached when they are written as one word (for example, `والكتاب` stays one token, not three!).
- Do not emit punctuation as a token.
- Number `position` consecutively from 0.
- Give each token a short gloss in the same language of the provided translation 
  (English as fallback if no translation is provided, BUT read always the sentence translation and identify the language first). 
  Use `null` only when no reliable gloss is possible.

Respond only with JSON following this exact structure. Example:

Input:
sentence: "فهو محتاج إلى مآخذ متعدّدة ومعارف متنوّعة"
translation: "quindi egli ha bisogno di molteplici fonti e conoscenze varie"

```json
{
  "tokens": [
    {
      "position": 0,
      "arabic": "فهو",
      "transliteration": "fa-huwa",
      "translation": "quindi egli"
    },
    {
      "position": 1,
      "arabic": "محتاج",
      "transliteration": "me7taj",
      "translation": "ha bisogno"
    },
    {
      "position": 2,
      "arabic": "إلى",
      "transliteration": "illa",
      "translation": "di/per"
    },
    {
      "position": 2,
      "arabic": "مآخذ",
      "transliteration": "ma'akhidh",
      "translation": "fonti"
    },
    {
      "position": 3, 
      "arabic": "متعدّدة",
      "transliteration": "muta3addida",
      "translation": "molteplici"
    },
    {
      "position": 4,
      "arabic": "ومعارف",
      "transliteration": "wa-ma3arif",
      "translation": "e conoscenze"
    },
    {
      "position": 5,
      "arabic": "متنوّعة",
      "transliteration": "mutanawwi3a",
      "translation": "varie"
    }
  ]
}
```

Sentence: "<arabicText>"
Translation: "<translation>"
