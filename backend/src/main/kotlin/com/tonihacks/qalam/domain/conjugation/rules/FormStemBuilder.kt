package com.tonihacks.qalam.domain.conjugation.rules

import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugation.model.SegmentType
import com.tonihacks.qalam.domain.word.VerbPattern

/**
 * Builds stems for Forms II–X sound triliteral verbs.
 *
 * Each form has a fixed pattern — no user-supplied vowel patterns needed (unlike Form I).
 *
 * Returns a [FormStem] containing:
 * - segments for the stem body (between prefix and suffix)
 * - the present prefix vowel (fatḥa or damma depending on form)
 * - whether the form adds an extra prefix to the present stem (e.g. Form V تـ, Form VII نـ)
 */
object FormStemBuilder {

    data class FormStem(
        val pastSegments: List<Segment>,
        val presentSegments: List<Segment>,
        val presentPrefixVowel: String,
    )

    fun build(form: VerbPattern, r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        return when (form) {
            VerbPattern.I -> throw IllegalArgumentException("Use FormIStemBuilder for Form I")
            VerbPattern.II -> buildFormII(r1, r2, r3, isPassive)
            VerbPattern.III -> buildFormIII(r1, r2, r3, isPassive)
            VerbPattern.IV -> buildFormIV(r1, r2, r3, isPassive)
            VerbPattern.V -> buildFormV(r1, r2, r3, isPassive)
            VerbPattern.VI -> buildFormVI(r1, r2, r3, isPassive)
            VerbPattern.VII -> buildFormVII(r1, r2, r3, isPassive)
            VerbPattern.VIII -> buildFormVIII(r1, r2, r3, isPassive)
            VerbPattern.IX -> buildFormIX(r1, r2, r3, isPassive)
            VerbPattern.X -> buildFormX(r1, r2, r3, isPassive)
        }
    }

    // Form II: fa33ala / yufa33ilu — R2 gets shadda
    // Past active: R1+fatḥa + R2+shadda+fatḥa + R3
    // Past passive: R1+damma + R2+shadda+kasra + R3
    // Present active: R1+fatḥa + R2+shadda+kasra + R3 (prefix: damma)
    // Present passive: R1+fatḥa + R2+shadda+fatḥa + R3 (prefix: damma)
    private fun buildFormII(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$FATHA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(DAMMA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$KASRA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$KASRA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$FATHA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        return FormStem(pastSegs, presSegs, DAMMA)
    }

    // Form III: faa3ala / yufaa3ilu — alif after R1
    // Past active: R1+fatḥa+alif + R2+fatḥa + R3
    // Past passive: R1+damma+alif + R2+kasra + R3
    // Present active: R1+fatḥa+alif + R2+kasra + R3 (prefix: damma)
    // Present passive: R1+fatḥa+alif + R2+fatḥa + R3 (prefix: damma)
    private fun buildFormIII(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${DAMMA}و", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        return FormStem(pastSegs, presSegs, DAMMA)
    }

    // Form IV: af3ala / yuf3ilu — hamza prefix in past
    // Past active: أَ + R1+sukun + R2+fatḥa + R3
    // Past passive: أُ + R1+sukun + R2+kasra + R3
    // Present active: R1+sukun + R2+kasra + R3 (prefix: damma, so يُ)
    // Present passive: R1+sukun + R2+fatḥa + R3 (prefix: damma)
    private fun buildFormIV(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment("أَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("أُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        return FormStem(pastSegs, presSegs, DAMMA)
    }

    // Form V: tafa33ala / yatafa33alu — ta prefix + R2 shadda
    // Past active: تَ + R1+fatḥa + R2+shadda+fatḥa + R3
    // Past passive: تُ + R1+damma + R2+shadda+kasra + R3
    // Present active: تَ + R1+fatḥa + R2+shadda+fatḥa + R3 (prefix: fatḥa, so يَ)
    // Present passive: تُ + R1+damma + R2+shadda+fatḥa + R3 (prefix: damma)
    private fun buildFormV(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment("تَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$FATHA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("تُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(DAMMA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$KASRA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment("تَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$FATHA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("تُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(DAMMA, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment("$SHADDA$FATHA", SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val prefixVowel = if (!isPassive) FATHA else DAMMA
        return FormStem(pastSegs, presSegs, prefixVowel)
    }

    // Form VI: tafaa3ala / yatafaa3alu — ta prefix + alif after R1
    // Past active: تَ + R1+fatḥa+alif + R2+fatḥa + R3
    // Present active: تَ + R1+fatḥa+alif + R2+fatḥa + R3 (prefix: fatḥa)
    private fun buildFormVI(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment("تَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("تُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${DAMMA}و", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment("تَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("تُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val prefixVowel = if (!isPassive) FATHA else DAMMA
        return FormStem(pastSegs, presSegs, prefixVowel)
    }

    // Form VII: infa3ala / yanfa3ilu — in prefix
    // Past active: اِنْ + R1+fatḥa + R2+fatḥa + R3
    // Present active: نْ + R1+fatḥa + R2+kasra + R3 (prefix: fatḥa, so يَ)
    // No passive for Form VII (already passive in meaning)
    private fun buildFormVII(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = listOf(
            Segment("ا${KASRA}نْ", SegmentType.PATTERN_VOWEL),
            Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
            Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
            Segment(r3, SegmentType.ROOT),
        )
        val presSegs = listOf(
            Segment("ن${SUKUN}", SegmentType.PATTERN_VOWEL),
            Segment(r1, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
            Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
            Segment(r3, SegmentType.ROOT),
        )
        return FormStem(pastSegs, presSegs, FATHA)
    }

    // Form VIII: ifta3ala / yafta3ilu — ta infix after R1
    // Past active: اِ + R1+تَ + R2+fatḥa + R3
    // Present active: R1+تَ + R2+kasra + R3 (prefix: fatḥa)
    // Past passive: اُ + R1+تُ + R2+kasra + R3
    // Present passive: R1+تَ + R2+fatḥa + R3 (prefix: damma)
    private fun buildFormVIII(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment("ا${KASRA}", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${SUKUN}تَ", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("ا${DAMMA}", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment("${SUKUN}تُ", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${SUKUN}تَ", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment(r1, SegmentType.ROOT), Segment("${SUKUN}تَ", SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val prefixVowel = if (!isPassive) FATHA else DAMMA
        return FormStem(pastSegs, presSegs, prefixVowel)
    }

    // Form IX: if3alla — color/defect verbs (rare, geminate R3)
    // Past active: اِ + R1+fatḥa + R2+fatḥa + R3+R3 (R3 doubled)
    // Present active: R1+fatḥa + R2+kasra + R3+R3 (prefix: fatḥa)
    // No passive
    private fun buildFormIX(r1: String, r2: String, r3: String, @Suppress("UnusedParameter") isPassive: Boolean): FormStem {
        val pastSegs = listOf(
            Segment("ا${KASRA}", SegmentType.PATTERN_VOWEL),
            Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
            Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
            Segment(r3, SegmentType.ROOT), Segment(SHADDA, SegmentType.PATTERN_VOWEL),
        )
        val presSegs = listOf(
            Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
            Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
            Segment(r3, SegmentType.ROOT), Segment(SHADDA, SegmentType.PATTERN_VOWEL),
        )
        return FormStem(pastSegs, presSegs, FATHA)
    }

    // Form X: istaf3ala / yastaf3ilu — ist prefix
    // Past active: اِسْتَ + R1+sukun + R2+fatḥa + R3
    // Past passive: اُسْتُ + R1+sukun + R2+kasra + R3
    // Present active: سْتَ + R1+sukun + R2+kasra + R3 (prefix: fatḥa, so يَ)
    // Present passive: سْتَ + R1+sukun + R2+fatḥa + R3 (prefix: damma)
    private fun buildFormX(r1: String, r2: String, r3: String, isPassive: Boolean): FormStem {
        val pastSegs = if (!isPassive) {
            listOf(
                Segment("ا${KASRA}سْتَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("ا${DAMMA}سْتُ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val presSegs = if (!isPassive) {
            listOf(
                Segment("سْتَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(KASRA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        } else {
            listOf(
                Segment("سْتَ", SegmentType.PATTERN_VOWEL),
                Segment(r1, SegmentType.ROOT), Segment(SUKUN, SegmentType.PATTERN_VOWEL),
                Segment(r2, SegmentType.ROOT), Segment(FATHA, SegmentType.PATTERN_VOWEL),
                Segment(r3, SegmentType.ROOT),
            )
        }
        val prefixVowel = if (!isPassive) FATHA else DAMMA
        return FormStem(pastSegs, presSegs, prefixVowel)
    }
}
