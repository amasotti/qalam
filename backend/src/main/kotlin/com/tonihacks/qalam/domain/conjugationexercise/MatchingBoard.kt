package com.tonihacks.qalam.domain.conjugationexercise

import com.tonihacks.qalam.domain.conjugation.model.ConjugationKey
import com.tonihacks.qalam.domain.conjugation.model.ConjugationTable
import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.PersonConjugation
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice

const val MATCHING_PAIR_COUNT = 4

/** Selects a stable subset; presentation order is shuffled separately when a session is created. */
fun ConjugationTable.matchingForms(
    tense: Tense,
    voice: Voice,
): List<PersonConjugation> =
    forms[ConjugationKey(tense, voice)]
        .orEmpty()
        .sortedBy { it.person.ordinal }
        .shuffled()
        .take(MATCHING_PAIR_COUNT)

fun Person.exerciseLabel(): String = when (this) {
    Person.FIRST_SINGULAR -> "1st singular"
    Person.SECOND_SINGULAR_MASC -> "2nd singular masculine"
    Person.SECOND_SINGULAR_FEM -> "2nd singular feminine"
    Person.THIRD_SINGULAR_MASC -> "3rd singular masculine"
    Person.THIRD_SINGULAR_FEM -> "3rd singular feminine"
    Person.SECOND_DUAL -> "2nd dual"
    Person.THIRD_DUAL_MASC -> "3rd dual masculine"
    Person.THIRD_DUAL_FEM -> "3rd dual feminine"
    Person.FIRST_PLURAL -> "1st plural"
    Person.SECOND_PLURAL_MASC -> "2nd plural masculine"
    Person.SECOND_PLURAL_FEM -> "2nd plural feminine"
    Person.THIRD_PLURAL_MASC -> "3rd plural masculine"
    Person.THIRD_PLURAL_FEM -> "3rd plural feminine"
}
