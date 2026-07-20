package com.tonihacks.qalam.domain.conjugation.rules

import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.PersonConjugation
import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugation.model.SegmentType
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice

/**
 * Applies stem modifications for weak (irregular) verb types.
 * These rules handle Form I only — Forms II-X regularize most weakness.
 */
object WeakVerbRules {

    // ── Hollow verbs (R2 = و or ي) ──────────────────────────────────────
    // e.g. قال (q-w-l): past قَالَ / قُلْتُ, present يَقُولُ / يَقُلْنَ

    /**
     * Past tense of hollow verb.
     * - Vowel-initial suffixes (3MS, 3FS, 3DM, 3DF, 3PM): long stem قَالَ
     * - Consonant-initial suffixes (1S, 2SM, 2SF, 2D, 1P, 2PM, 2PF, 3PF): contracted stem قُلْ
     */
    @Suppress("UnusedParameter") // r2 unused for hollow (it's the weak radical that transforms)
    fun conjugateHollowPast(
        r1: String,
        r2: String,
        r3: String,
        person: Person,
        voice: Voice,
    ): PersonConjugation {
        val suffix = PAST_SUFFIXES[person]!!
        val segments = mutableListOf<Segment>()
        val isContracted = person in PAST_R3_GETS_SUKUN

        if (isContracted) {
            // Contracted stem: R1 + damma + R3 + suffix (sukūn on R3 is in suffix)
            val r1Vowel = if (voice == Voice.PASSIVE) KASRA else DAMMA
            segments.add(Segment(r1, SegmentType.ROOT))
            segments.add(Segment(r1Vowel, SegmentType.PATTERN_VOWEL))
            segments.add(Segment(r3, SegmentType.ROOT))
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        } else {
            // Long stem: R1 + fatḥa + alif + R3 (+ suffix)
            if (voice == Voice.PASSIVE) {
                // Passive hollow past: قِيلَ — R1 + kasra + yā + R3
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment("${KASRA}ي", SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            } else {
                // Active: R1 + fatḥa + alif + R3
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            }
            if (suffix.isNotEmpty()) {
                segments.add(Segment(suffix, SegmentType.SUFFIX))
            } else {
                segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
            }
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    /**
     * Present tense of hollow verb.
     * - Most persons: prefix + R1 + long vowel (wāw/yā) + R3 + suffix
     * - 2PF/3PF (sukūn-initial suffix): prefix + R1 + damma/kasra + R3 + suffix (contracted)
     */
    @Suppress("UnusedParameter") // r2 unused for hollow (it's the weak radical that transforms)
    fun conjugateHollowPresent(
        r1: String,
        r2: String,
        r3: String,
        person: Person,
        voice: Voice,
        presentPattern: String?,
    ): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val suffix = PRESENT_SUFFIXES_INDICATIVE[person]!!
        val segments = mutableListOf<Segment>()

        val prefix = if (voice == Voice.PASSIVE) {
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            prefixTemplate
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        // Is this a contracted form? (2PF, 3PF have sukūn-initial suffixes)
        val isContracted = person == Person.SECOND_PLURAL_FEM || person == Person.THIRD_PLURAL_FEM

        if (isContracted) {
            if (voice == Voice.PASSIVE) {
                // Passive contracted: prefix + R1 + fatḥa + R3 + suffix
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            } else {
                // Active contracted: prefix + R1 + damma/kasra + R3 + suffix
                val vowel = FormIStemBuilder.extractPresentVowel(presentPattern)
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment(vowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            }
        } else {
            if (voice == Voice.PASSIVE) {
                // Passive: prefix + R1 + fatḥa + alif + R3
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment("${FATHA}ا", SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            } else {
                // Active: prefix + R1 + long vowel + R3
                // Present vowel determines the long vowel: u → وُ, i → ِي, a → َا
                val presentVowel = FormIStemBuilder.extractPresentVowel(presentPattern)
                val longVowel = when (presentVowel) {
                    DAMMA -> "${DAMMA}و"
                    KASRA -> "${KASRA}ي"
                    else -> "${FATHA}ا"
                }
                segments.add(Segment(r1, SegmentType.ROOT))
                segments.add(Segment(longVowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment(r3, SegmentType.ROOT))
            }
        }

        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    // ── Defective verbs (R3 = و or ي) ───────────────────────────────────
    // e.g. مشى (m-sh-y): past مَشَى / مَشَيْتُ, present يَمْشِي / يَمْشُونَ

    fun conjugateDefectivePast(
        r1: String,
        r2: String,
        r3: String,
        person: Person,
        voice: Voice,
        pastPattern: String?,
    ): PersonConjugation {
        val suffix = PAST_SUFFIXES[person]!!
        val segments = mutableListOf<Segment>()
        val r1Vowel = if (voice == Voice.PASSIVE) DAMMA else FATHA
        val r2Vowel = if (voice == Voice.PASSIVE) KASRA else FormIStemBuilder.extractPastVowel(pastPattern)

        segments.add(Segment(r1, SegmentType.ROOT))
        segments.add(Segment(r1Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(r2, SegmentType.ROOT))
        segments.add(Segment(r2Vowel, SegmentType.PATTERN_VOWEL))

        when (person) {
            Person.THIRD_SINGULAR_MASC -> {
                // 3MS: مَشَى — R3 becomes alif maqsura (ى) or alif (ا) depending on R3
                if (voice == Voice.PASSIVE) {
                    // مُشِيَ
                    segments.add(Segment("ي", SegmentType.ROOT))
                    segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
                } else {
                    val finalAlif = if (r3 == "ي") "ى" else "ا"
                    segments.add(Segment(finalAlif, SegmentType.PATTERN_VOWEL))
                }
            }
            Person.THIRD_SINGULAR_FEM -> {
                // 3FS: مَشَتْ — R3 drops, fatḥa on R2 remains, + تْ
                segments.add(Segment("تْ", SegmentType.SUFFIX))
            }
            Person.THIRD_DUAL_MASC -> {
                // 3DM: مَشَيَا — R3 appears as yā + fatḥa + alif
                segments.add(Segment(r3, SegmentType.ROOT))
                segments.add(Segment("َا", SegmentType.SUFFIX))
            }
            Person.THIRD_DUAL_FEM -> {
                // 3DF: مَشَتَا — R3 drops, + تَا
                segments.add(Segment("تَا", SegmentType.SUFFIX))
            }
            Person.THIRD_PLURAL_MASC -> {
                // 3PM: مَشَوْا — R3 becomes wāw + sukūn + alif
                segments.add(Segment("وْا", SegmentType.SUFFIX))
            }
            Person.THIRD_PLURAL_FEM -> {
                // 3PF: مَشَيْنَ — R3 as yā + sukūn + نَ
                segments.add(Segment(r3, SegmentType.ROOT))
                segments.add(Segment("${SUKUN}نَ", SegmentType.SUFFIX))
            }
            else -> {
                // Consonant-initial suffixes (1S, 2SM, 2SF, 2D, 1P, 2PM, 2PF):
                // مَشَيْتُ — R3 as yā + sukūn (in suffix)
                segments.add(Segment(r3, SegmentType.ROOT))
                segments.add(Segment(suffix, SegmentType.SUFFIX))
            }
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    @Suppress("UnusedParameter") // r3 unused in present (weak R3 is handled by suffix logic)
    fun conjugateDefectivePresent(
        r1: String,
        r2: String,
        r3: String,
        person: Person,
        voice: Voice,
        presentPattern: String?,
    ): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val segments = mutableListOf<Segment>()

        val prefix = if (voice == Voice.PASSIVE) {
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            prefixTemplate
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        // Stem: R1 + sukūn + R2
        segments.add(Segment(r1, SegmentType.ROOT))
        segments.add(Segment(SUKUN, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(r2, SegmentType.ROOT))

        val presentVowel = if (voice == Voice.PASSIVE) FATHA
        else FormIStemBuilder.extractPresentVowel(presentPattern)

        when (person) {
            // Persons with vowel-type suffix that replaces R3:
            Person.SECOND_PLURAL_MASC, Person.THIRD_PLURAL_MASC -> {
                // يَمْشُونَ — R2 gets damma, + ونَ
                segments.add(Segment(DAMMA, SegmentType.PATTERN_VOWEL))
                segments.add(Segment("ونَ", SegmentType.SUFFIX))
            }
            Person.SECOND_SINGULAR_FEM -> {
                // تَمْشِينَ — R2 + kasra + ينَ (same as regular suffix but R3 absorbed)
                segments.add(Segment(presentVowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment("ينَ", SegmentType.SUFFIX))
            }
            Person.SECOND_PLURAL_FEM, Person.THIRD_PLURAL_FEM -> {
                // تَمْشِينَ (2PF) / يَمْشِينَ (3PF) — R2 + kasra + ينَ
                segments.add(Segment(presentVowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment("ينَ", SegmentType.SUFFIX))
            }
            Person.SECOND_DUAL, Person.THIRD_DUAL_MASC, Person.THIRD_DUAL_FEM -> {
                // تَمْشِيَانِ — R2 + kasra + yā + fatḥa + alif + nūn + kasra
                segments.add(Segment(presentVowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment("يَانِ", SegmentType.SUFFIX))
            }
            else -> {
                // 1S, 2SM, 3SM, 3SF, 1P: يَمْشِي — R2 + present vowel + yā (no extra suffix)
                segments.add(Segment(presentVowel, SegmentType.PATTERN_VOWEL))
                segments.add(Segment("ي", SegmentType.PATTERN_VOWEL))
            }
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    // ── Geminate verbs (R2 = R3) — handled by applyGeminateContraction below ────

    // ── Assimilated verbs (R1 = و or ي) ─────────────────────────────────
    // e.g. وصل (w-s-l): past is regular (وَصَلَ), present drops R1 (يَصِلُ)

    /**
     * Past tense of assimilated verbs is regular — delegates to sound verb logic.
     * R1 (wāw) stays in past tense.
     */
    // No special past method needed — use sound verb past.

    /**
     * Present tense of assimilated verb: R1 (wāw) drops.
     * يَوْصِلُ → يَصِلُ
     */
    @Suppress("UnusedParameter") // r1 unused in present (assimilated R1 drops)
    fun conjugateAssimilatedPresent(
        r1: String,
        r2: String,
        r3: String,
        person: Person,
        voice: Voice,
        presentPattern: String?,
    ): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val suffix = PRESENT_SUFFIXES_INDICATIVE[person]!!
        val segments = mutableListOf<Segment>()

        val prefix = if (voice == Voice.PASSIVE) {
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            prefixTemplate
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        // R1 drops in present! Stem is just R2 + vowel + R3
        val r2Vowel = if (voice == Voice.PASSIVE) FATHA
        else FormIStemBuilder.extractPresentVowel(presentPattern)

        segments.add(Segment(r2, SegmentType.ROOT))
        segments.add(Segment(r2Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(r3, SegmentType.ROOT))

        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }
}

/**
 * Applies geminate contraction to a [PersonConjugation] built for a verb with R2 = R3.
 *
 * Scans segments for the last two ROOT segments (R2, R3).  When the segment immediately
 * after R3 begins with a short-vowel diacritic (fatḥa U+064E, ḍamma U+064F, kasra U+0650),
 * contraction occurs:
 *   - R1's vowel segment is replaced with R2's vowel (vowel-shift rule)
 *   - R3 is dropped; R2 acquires SHADDA + the leading vowel of the following segment
 *   - The remainder of the following segment, if non-empty, is appended as a suffix
 *
 * When the following segment begins with sukūn (U+0652) or shadda (U+0651) — or when the
 * structure is unexpected — the input is returned unchanged (uncontracted form, e.g. قَرَرْتُ).
 *
 * Reference: Wright §194-196 (rules for the doubled verb, الفعل المضاعف).
 */
fun applyGeminateContraction(conjugation: PersonConjugation): PersonConjugation {
    val segs = conjugation.segments

    // Last ROOT segment = R3 in the geminate stem
    val j = segs.indexOfLast { it.type == SegmentType.ROOT }
    if (j < 0 || j + 1 >= segs.size) return conjugation

    // The segment after R3 holds the suffix (or the 3SM fatḥa added as PATTERN_VOWEL)
    val afterR3 = segs[j + 1]
    val firstChar = afterR3.text.firstOrNull() ?: return conjugation

    // Only contract when the leading character is a short-vowel diacritic
    if (firstChar.code !in 0x064E..0x0650) return conjugation

    val contractionVowel = firstChar.toString()
    val suffixRest = afterR3.text.drop(1)

    // Second-to-last ROOT = R2; expect exactly one PATTERN_VOWEL between R2 and R3
    val k = segs.subList(0, j).indexOfLast { it.type == SegmentType.ROOT }
    if (k < 1 || j != k + 2) return conjugation   // unexpected structure

    val r2Vowel = segs[k + 1].text  // PATTERN_VOWEL at k+1, between R2 and R3

    val result = mutableListOf<Segment>()
    for (i in 0 until k - 1) result.add(segs[i])            // everything before R1_vowel
    result.add(Segment(r2Vowel, SegmentType.PATTERN_VOWEL))  // R1 takes R2's vowel
    result.add(segs[k])                                       // R2 ROOT unchanged
    result.add(Segment(contractionVowel + SHADDA, SegmentType.PATTERN_VOWEL)) // vowel + shadda (matches existing convention, e.g. FormStemBuilder "$FATHA$SHADDA")
    // segs[k+1] (R2_vowel), segs[j] (R3), segs[j+1] (after-R3) are consumed
    if (suffixRest.isNotEmpty()) result.add(Segment(suffixRest, SegmentType.SUFFIX))
    for (i in j + 2 until segs.size) result.add(segs[i])     // any trailing segments

    val arabic = result.joinToString("") { it.text }
    return PersonConjugation(conjugation.person, arabic, result)
}
