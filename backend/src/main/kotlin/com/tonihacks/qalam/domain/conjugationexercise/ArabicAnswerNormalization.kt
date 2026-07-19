package com.tonihacks.qalam.domain.conjugationexercise

import java.text.Normalizer

fun normalizeArabicExerciseAnswer(value: String): String =
    Normalizer.normalize(value.trim().replace("ـ", ""), Normalizer.Form.NFC)
