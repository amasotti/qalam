package com.tonihacks.qalam.domain.conjugation.rules

import com.tonihacks.qalam.domain.conjugation.model.Person

// --- Arabic diacritics constants ---
const val FATHA = "\u064E"       // َ
const val DAMMA = "\u064F"       // ُ
const val KASRA = "\u0650"       // ِ
const val SUKUN = "\u0652"       // ْ
const val SHADDA = "\u0651"      // ّ

/**
 * Past tense suffix per person.
 * The suffix attaches to the past stem (R1+fatḥa+R2+vowel+R3).
 * For persons that use a consonant suffix, R3 gets sukūn before the suffix.
 * For 3MS: no suffix (stem-final fatḥa). For 3SM: stem ends with fatḥa on R3.
 */
val PAST_SUFFIXES: Map<Person, String> = mapOf(
    Person.FIRST_SINGULAR        to "${SUKUN}تُ",
    Person.SECOND_SINGULAR_MASC  to "${SUKUN}تَ",
    Person.SECOND_SINGULAR_FEM   to "${SUKUN}تِ",
    Person.THIRD_SINGULAR_MASC   to "",            // stem-final fatḥa on R3
    Person.THIRD_SINGULAR_FEM    to "َتْ",          // fatḥa + tā + sukūn
    Person.SECOND_DUAL           to "${SUKUN}تُمَا",
    Person.THIRD_DUAL_MASC       to "َا",           // fatḥa + alif
    Person.THIRD_DUAL_FEM        to "َتَا",          // fatḥa + tā + alif
    Person.FIRST_PLURAL          to "${SUKUN}نَا",
    Person.SECOND_PLURAL_MASC    to "${SUKUN}تُمْ",
    Person.SECOND_PLURAL_FEM     to "${SUKUN}تُنَّ",
    Person.THIRD_PLURAL_MASC     to "ُوا",          // damma + wāw + alif
    Person.THIRD_PLURAL_FEM      to "${SUKUN}نَ",
)

/**
 * Present tense prefix per person.
 * Format: prefix consonant + fatḥa (for Form I).
 */
val PRESENT_PREFIXES: Map<Person, String> = mapOf(
    Person.FIRST_SINGULAR        to "أَ",
    Person.SECOND_SINGULAR_MASC  to "تَ",
    Person.SECOND_SINGULAR_FEM   to "تَ",
    Person.THIRD_SINGULAR_MASC   to "يَ",
    Person.THIRD_SINGULAR_FEM    to "تَ",
    Person.SECOND_DUAL           to "تَ",
    Person.THIRD_DUAL_MASC       to "يَ",
    Person.THIRD_DUAL_FEM        to "تَ",
    Person.FIRST_PLURAL          to "نَ",
    Person.SECOND_PLURAL_MASC    to "تَ",
    Person.SECOND_PLURAL_FEM     to "تَ",
    Person.THIRD_PLURAL_MASC     to "يَ",
    Person.THIRD_PLURAL_FEM      to "يَ",
)

/**
 * Present tense suffix per person (indicative mood).
 * Most persons: damma on final consonant. Some add extra letters.
 */
val PRESENT_SUFFIXES_INDICATIVE: Map<Person, String> = mapOf(
    Person.FIRST_SINGULAR        to "ُ",
    Person.SECOND_SINGULAR_MASC  to "ُ",
    Person.SECOND_SINGULAR_FEM   to "ِينَ",
    Person.THIRD_SINGULAR_MASC   to "ُ",
    Person.THIRD_SINGULAR_FEM    to "ُ",
    Person.SECOND_DUAL           to "َانِ",
    Person.THIRD_DUAL_MASC       to "َانِ",
    Person.THIRD_DUAL_FEM        to "َانِ",
    Person.FIRST_PLURAL          to "ُ",
    Person.SECOND_PLURAL_MASC    to "ُونَ",
    Person.SECOND_PLURAL_FEM     to "${SUKUN}نَ",
    Person.THIRD_PLURAL_MASC     to "ُونَ",
    Person.THIRD_PLURAL_FEM      to "${SUKUN}نَ",
)

/**
 * Determines if a past suffix starts with a consonant (needs sukūn on R3).
 * Persons: 1S, 2SM, 2SF, 2D, 1P, 2PM, 2PF, 3PF — their suffixes start with sukūn.
 * Persons: 3SM (no suffix), 3SF, 3DM, 3DF, 3PM — R3 keeps its vowel.
 */
val PAST_R3_GETS_SUKUN: Set<Person> = setOf(
    Person.FIRST_SINGULAR,
    Person.SECOND_SINGULAR_MASC,
    Person.SECOND_SINGULAR_FEM,
    Person.SECOND_DUAL,
    Person.FIRST_PLURAL,
    Person.SECOND_PLURAL_MASC,
    Person.SECOND_PLURAL_FEM,
    Person.THIRD_PLURAL_FEM,
)
