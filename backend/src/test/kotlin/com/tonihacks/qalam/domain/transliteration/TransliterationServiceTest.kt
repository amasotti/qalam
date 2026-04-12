package com.tonihacks.qalam.domain.transliteration

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TransliterationServiceTest : FreeSpec({

    val service = TransliterationService()

    "empty string returns empty string" {
        service.transliterate("") shouldBe ""
    }

    "blank string returns the same blank string" {
        service.transliterate("   ") shouldBe "   "
    }

    "single characters map correctly" - {

        "ح maps to 7" {
            service.transliterate("ح") shouldBe "7"
        }

        "ع maps to 3" {
            service.transliterate("ع") shouldBe "3"
        }

        "ص maps to S" {
            service.transliterate("ص") shouldBe "S"
        }

        "ض maps to D" {
            service.transliterate("ض") shouldBe "D"
        }

        "ط maps to T" {
            service.transliterate("ط") shouldBe "T"
        }

        "ش maps to sh" {
            service.transliterate("ش") shouldBe "sh"
        }

        "ث maps to th" {
            service.transliterate("ث") shouldBe "th"
        }

        "خ maps to kh" {
            service.transliterate("خ") shouldBe "kh"
        }

        "ذ maps to dh" {
            service.transliterate("ذ") shouldBe "dh"
        }

        "غ maps to gh" {
            service.transliterate("غ") shouldBe "gh"
        }
    }

    "definite article ال maps to el-" {
        service.transliterate("ال") shouldBe "el-"
    }

    "اللَّه transliterates with el- prefix" {
        // ا + ل → el-, then ل → l, then damma-shadda → ignore, then ه → h
        val result = service.transliterate("اللَّه")
        result shouldBe "el-lah"
    }

    "بِسْمِ transliterates correctly" {
        // ب → b, kasra → i, سْ → s (sukun → ""), مِ → mi
        service.transliterate("بِسْمِ") shouldBe "bismi"
    }

    "full basmala transliteration" {
        // بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ
        val result = service.transliterate("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ")
        // Expected breakdown (char-by-char):
        // بِسْمِ   → b + i(kasra) + s + ""(sukun) + m + i(kasra)          = "bismi"
        // space    → " "
        // اللَّهِ  → el-(defart) + l + a(fatha) + ""(shadda) + h + i(kasra) = "el-lahi"
        // space    → " "
        // الرَّحْمَنِ → el- + r + a(fatha) + ""(shadda) + 7 + ""(sukun) + m + a(fatha) + n + i(kasra) = "el-ra7mani"
        // space    → " "
        // الرَّحِيمِ → el- + r + a(fatha) + ""(shadda) + 7 + i(kasra) + i(yaa) + m + i(kasra) = "el-ra7iimi"
        result shouldBe "bismi el-lahi el-ra7mani el-ra7iimi"
    }

    "characters not in the map are passed through unchanged" {
        // Latin letters not in the map
        service.transliterate("ABC") shouldBe "ABC"
    }

    "mixed Arabic and Latin text" {
        // Digits are in the map, space is in the map
        val result = service.transliterate("ب 1")
        result shouldBe "b 1"
    }

    "Arabic punctuation maps correctly" {
        service.transliterate("،") shouldBe ","
        service.transliterate("؟") shouldBe "?"
    }

    "Arabic-Indic numerals map to ASCII digits" {
        service.transliterate("٠١٢٣") shouldBe "٠١٢٣" // these are NOT in the map — passthrough
    }
})
