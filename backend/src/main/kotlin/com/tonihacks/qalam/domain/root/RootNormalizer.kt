package com.tonihacks.qalam.domain.root

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.toTokens
import com.tonihacks.qalam.domain.validateConsonants

/**
 * Canonicalizes Arabic root input regardless of how the letters are delimited.
 *
 * Accepted formats:
 *   "رحب"      — concatenated (no separator)
 *   "ر ح ب"    — space-separated
 *   "ر-ح-ب"    — dash-separated
 *   "ر,ح,ب"    — comma-separated
 *
 * Each letter must be a single Arabic consonant (U+0621–U+064A).
 * Valid letter count: 2–6, although already six is borderline and is usually a sign of loan root (e.g. from Berber).
 */
data class NormalizedRoot(
    val letters: List<String>,
    val normalizedForm: String,
    val displayForm: String,
    val letterCount: Int,
)

object RootNormalizer {

    private const val MIN_ROOT_LENGTH = 2
    private const val MAX_ROOT_LENGTH = 6

    fun normalize(input: String): Either<DomainError.InvalidInput, NormalizedRoot> {
        return input.toTokens()
            .flatMap { it.validateConsonants() } // Should consist of Arabic script consonants only
            .flatMap { it.validateLength() } // 2-6 letters
            .map { it.buildNormalizedRoot() } // Build the NormalizedRoot object
    }

    // --- Logic Steps ---

    private fun List<String>.validateLength(): Either<DomainError.InvalidInput, List<String>> {
        return if (size !in MIN_ROOT_LENGTH..MAX_ROOT_LENGTH) {
            DomainError.InvalidInput("Root must have ${MIN_ROOT_LENGTH}–${MAX_ROOT_LENGTH} letters, got $size").left()
        } else {
            this.right()
        }
    }

    private fun List<String>.buildNormalizedRoot() = NormalizedRoot(
        letters = this,
        normalizedForm = joinToString(""),
        displayForm = joinToString("-"),
        letterCount = size
    )
}
