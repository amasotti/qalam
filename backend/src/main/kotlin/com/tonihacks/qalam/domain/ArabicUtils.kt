package com.tonihacks.qalam.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootNormalizer

// Arabic consonants: ء (U+0621) through ي (U+064A).
// This range excludes diacritics (U+064B+) and non-Arabic characters.
private val ARABIC_CONSONANT = Regex("^[\u0621-\u064A]$")
private val DELIMITERS = Regex("[ \\-,_،/|]+")

/**
 * Checks if the string is a single Arabic consonant (U+0621–U+064A).
 */
fun String.isArabicConsonant(): Boolean = ARABIC_CONSONANT.matches(this)


fun String.toTokens(): Either<DomainError.InvalidInput, List<String>> {
    val trimmed = this.trim()
    if (trimmed.isBlank()) return DomainError.InvalidInput("Input must not be blank").left()

    return if (trimmed.contains(DELIMITERS)) {
        trimmed.split(DELIMITERS).filter { it.isNotEmpty() }.right()
    } else {
        trimmed.map { it.toString() }.right()
    }
}

fun List<String>.validateConsonants(): Either<DomainError.InvalidInput, List<String>> {
    val invalid = firstOrNull { !it.isArabicConsonant() }
    return if (invalid != null) {
        DomainError.InvalidInput("'$invalid' is not a valid Arabic consonant").left()
    } else {
        this.right()
    }
}

