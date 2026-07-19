package com.tonihacks.qalam.domain.conjugationexercise

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArabicAnswerNormalizationTest : FunSpec({
    test("accepts NFC/NFD equivalent diacritic order and ignores presentation whitespace/tatweel") {
        normalizeArabicExerciseAnswer(" أُرِيدُـ ") shouldBe normalizeArabicExerciseAnswer("أُرِيدُ")
    }
    test("retains meaningful Arabic letters and diacritics") {
        normalizeArabicExerciseAnswer("كَتَبْتُ") shouldBe "كَتَبْتُ"
        normalizeArabicExerciseAnswer("كتبت") shouldBe "كتبت"
    }
})
