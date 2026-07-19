package com.tonihacks.qalam.domain.conjugation.rules

/**
 * Builds Form I stems from root letters and vowel patterns.
 *
 * Past pattern uses transliteration convention: fa3ala, fa3ila, fa3ula
 * where the vowel after '3' (= ع = R2) is the distinguishing vowel.
 *
 * Present pattern: yaf3ulu, yaf3ilu, yaf3alu
 * where the vowel after '3' (= R2) is the distinguishing vowel.
 */
object FormIStemBuilder {

    private val PAST_VOWEL_MAP = mapOf(
        'a' to FATHA,
        'i' to KASRA,
        'u' to DAMMA,
    )

    private val PRESENT_VOWEL_MAP = PAST_VOWEL_MAP

    /**
     * Extract the R2 vowel from a past pattern like "fa3ala", "fa3ila", "fa3ula".
     * Returns the Arabic diacritic.
     */
    fun extractPastVowel(pastPattern: String?): String {
        if (pastPattern == null) return FATHA  // default: fa3ala
        // Pattern: fa3Xla — vowel X is at index 4 (0-based)
        val normalized = pastPattern.lowercase().trim()
        // Find the vowel after '3': fa3_la
        val idx = normalized.indexOf('3')
        if (idx >= 0 && idx + 1 < normalized.length) {
            val vowelChar = normalized[idx + 1]
            return PAST_VOWEL_MAP[vowelChar] ?: FATHA
        }
        return FATHA
    }

    /**
     * Extract the R2 vowel from a present pattern like "yaf3ulu", "yaf3ilu", "yaf3alu".
     * Returns the Arabic diacritic.
     */
    fun extractPresentVowel(presentPattern: String?): String {
        if (presentPattern == null) return DAMMA  // default: yaf3ulu
        val normalized = presentPattern.lowercase().trim()
        val idx = normalized.indexOf('3')
        if (idx >= 0 && idx + 1 < normalized.length) {
            val vowelChar = normalized[idx + 1]
            return PRESENT_VOWEL_MAP[vowelChar] ?: DAMMA
        }
        return DAMMA
    }

    /**
     * Build the past active stem for Form I sound verb.
     *
     * Structure: R1 + fatḥa + R2 + pastVowel + R3
     * (R3's final vowel is handled by the affix logic — fatḥa for 3MS, sukūn for consonant-initial suffixes, etc.)
     *
     * Returns: list of segments as (text, isRoot) pairs for later segment splitting.
     * Format: [(R1, root), (fatḥa, vowel), (R2, root), (pastVowel, vowel), (R3, root)]
     */
    data class StemParts(
        val r1: String,
        val r1Vowel: String,
        val r2: String,
        val r2Vowel: String,
        val r3: String,
    )

    fun buildPastActiveStem(rootLetters: List<String>, pastPattern: String?): StemParts {
        val pastVowel = extractPastVowel(pastPattern)
        return StemParts(
            r1 = rootLetters[0],
            r1Vowel = FATHA,    // R1 always gets fatḥa in Form I past active
            r2 = rootLetters[1],
            r2Vowel = pastVowel,
            r3 = rootLetters[2],
        )
    }

    /**
     * Build the past passive stem for Form I sound verb.
     *
     * Structure: R1 + damma + R2 + kasra + R3
     * (passive past always uses u-i pattern: fu3ila)
     */
    fun buildPastPassiveStem(rootLetters: List<String>): StemParts {
        return StemParts(
            r1 = rootLetters[0],
            r1Vowel = DAMMA,
            r2 = rootLetters[1],
            r2Vowel = KASRA,
            r3 = rootLetters[2],
        )
    }

    /**
     * Build the present active stem for Form I sound verb.
     *
     * Structure: R1 + sukūn + R2 + presentVowel + R3
     * (prefix is added separately)
     */
    fun buildPresentActiveStem(rootLetters: List<String>, presentPattern: String?): StemParts {
        val presentVowel = extractPresentVowel(presentPattern)
        return StemParts(
            r1 = rootLetters[0],
            r1Vowel = SUKUN,
            r2 = rootLetters[1],
            r2Vowel = presentVowel,
            r3 = rootLetters[2],
        )
    }

    /**
     * Build the present passive stem for Form I sound verb.
     *
     * Structure: R1 + sukūn + R2 + fatḥa + R3
     * Prefix vowel changes to damma (handled in engine, not here).
     * Pattern: yuf3alu
     */
    fun buildPresentPassiveStem(rootLetters: List<String>): StemParts {
        return StemParts(
            r1 = rootLetters[0],
            r1Vowel = SUKUN,
            r2 = rootLetters[1],
            r2Vowel = FATHA,
            r3 = rootLetters[2],
        )
    }
}
