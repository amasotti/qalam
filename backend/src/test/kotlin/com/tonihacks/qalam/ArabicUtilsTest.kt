package com.tonihacks.qalam

import com.tonihacks.qalam.domain.removeArabicDiacritics
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ArabicUtilsTest : StringSpec({

    "removes diacritics but keeps letters and punctuation" {
        "بِنايَ.".removeArabicDiacritics() shouldBe "بناي."
    }

    "leaves a string with no diacritics untouched" {
        "بناية".removeArabicDiacritics() shouldBe "بناية"
    }

    "strips shadda + tanwin combos" {
        "مُحَمَّدٌ".removeArabicDiacritics() shouldBe "محمد"
    }
})
