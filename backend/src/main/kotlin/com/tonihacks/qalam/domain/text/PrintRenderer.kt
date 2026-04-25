package com.tonihacks.qalam.domain.text

import com.tonihacks.qalam.domain.sentence.Sentence
import java.time.LocalDate

fun renderPrintHtml(text: Text, sentences: List<Sentence>): String {
    val date = LocalDate.now().toString()

    val tagsHtml = text.tags.joinToString("") { "<span class=\"tag\">${it.escapeHtml()}</span>" }

    val sentencesHtml = sentences.sortedBy { it.position }.joinToString("\n") { sentence ->
        buildString {
            append("<div class=\"sentence\">")
            append("<p class=\"arabic\" dir=\"rtl\">${sentence.arabicText.escapeHtml()}</p>")
            sentence.transliteration?.let { append("<p class=\"transliteration\">${it.escapeHtml()}</p>") }
            sentence.freeTranslation?.let { append("<p class=\"translation\">${it.escapeHtml()}</p>") }
            sentence.notes?.let { append("<p class=\"notes\">${it.escapeHtml()}</p>") }
            append("</div>")
        }
    }

    return """<!DOCTYPE html>
<html lang="ar">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${text.title.escapeHtml()}</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Noto+Naskh+Arabic:wght@400;700&family=Inter:wght@400;500;600&display=swap');

    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: 'Inter', sans-serif;
      max-width: 800px;
      margin: 0 auto;
      padding: 2rem;
      color: #1a1a1a;
      line-height: 1.6;
    }

    .header {
      border-bottom: 2px solid #1a1a1a;
      padding-bottom: 1.5rem;
      margin-bottom: 2rem;
    }

    .header h1 { font-size: 2rem; font-weight: 600; margin-bottom: 0.5rem; }

    .meta {
      display: flex;
      gap: 1rem;
      font-size: 0.85rem;
      color: #555;
      margin-bottom: 0.75rem;
      flex-wrap: wrap;
    }

    .description { font-size: 0.95rem; color: #333; margin-bottom: 0.75rem; }

    .tags { display: flex; gap: 0.4rem; flex-wrap: wrap; margin-top: 0.5rem; }

    .tag {
      background: #f0f0f0;
      border-radius: 3px;
      padding: 0.1rem 0.5rem;
      font-size: 0.8rem;
      color: #444;
    }

    .full-text {
      background: #fafafa;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      padding: 1.5rem;
      margin-bottom: 2.5rem;
      font-family: 'Noto Naskh Arabic', serif;
      font-size: 1.4rem;
      line-height: 2;
      direction: rtl;
      text-align: right;
    }

    .full-text-label {
      font-family: 'Inter', sans-serif;
      font-size: 0.75rem;
      color: #888;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 1rem;
      direction: ltr;
      text-align: left;
    }

    .sentences-section h2 {
      font-size: 1rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #888;
      margin-bottom: 1.5rem;
      border-bottom: 1px solid #e0e0e0;
      padding-bottom: 0.5rem;
    }

    .sentence {
      margin-bottom: 1.5rem;
      padding-bottom: 1.5rem;
      border-bottom: 1px solid #f0f0f0;
    }

    .sentence:last-child { border-bottom: none; }

    .sentence .arabic {
      font-family: 'Noto Naskh Arabic', serif;
      font-size: 1.3rem;
      line-height: 2;
      direction: rtl;
      text-align: right;
      margin-bottom: 0.3rem;
    }

    .sentence .transliteration { font-style: italic; color: #555; font-size: 0.9rem; margin-bottom: 0.2rem; }
    .sentence .translation { color: #333; font-size: 0.95rem; margin-bottom: 0.2rem; }
    .sentence .notes { color: #777; font-size: 0.82rem; font-style: italic; }

    @media print {
      body { padding: 0; max-width: 100%; }
      .sentence { page-break-inside: avoid; }
      .full-text { page-break-inside: avoid; }
    }
  </style>
</head>
<body>
  <header class="header">
    <h1>${text.title.escapeHtml()}</h1>
    <div class="meta">
      <span>${text.dialect.name.lowercase().replaceFirstChar { it.uppercase() }}</span>
      <span>${text.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}</span>
      <span>$date</span>
    </div>
    ${text.comments?.let { "<p class=\"description\">${it.escapeHtml()}</p>" } ?: ""}
    ${if (text.tags.isNotEmpty()) "<div class=\"tags\">$tagsHtml</div>" else ""}
  </header>

  <section class="full-text">
    <div class="full-text-label">النص الكامل</div>
    ${text.body.escapeHtml().replace("\n", "<br>")}
  </section>

  <section class="sentences-section">
    <h2>Sentence breakdown</h2>
    $sentencesHtml
  </section>
</body>
</html>"""
}

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
