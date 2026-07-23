Split this Arabic sentence into its orthographic word tokens.

- Keep clitics attached when they are written as one word (for example, `والكتاب` stays one token).
- Do not emit punctuation as a token.
- Number `position` consecutively from 0.
- Give each token a short gloss in the same language of the provided translation (English as fallback if no translation is provided). 
  Use `null` only when no reliable gloss is possible.

Respond only with JSON following this exact structure:

```json
{
  "tokens": [
    {
      "position": 0,
      "arabic": "أنا",
      "transliteration": "ana",
      "translation": "I"
    },
    {
      "position": 1,
      "arabic": "أقرأ",
      "transliteration": "aqra",
      "translation": "I read"
    }
  ]
}
```

Sentence: "<arabicText>"
Translation: "<translation>"
