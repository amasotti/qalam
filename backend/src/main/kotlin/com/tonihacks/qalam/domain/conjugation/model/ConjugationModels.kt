package com.tonihacks.qalam.domain.conjugation.model

enum class Tense { PAST, PRESENT }
enum class Voice { ACTIVE, PASSIVE }

enum class SegmentType { PREFIX, ROOT, PATTERN_VOWEL, SUFFIX }

data class Segment(val text: String, val type: SegmentType)

data class PersonConjugation(
    val person: Person,
    val arabic: String,
    val segments: List<Segment>,
)

/** Key for one sub-table: tense × voice. */
data class ConjugationKey(val tense: Tense, val voice: Voice)

/** Full conjugation result: map of (tense, voice) → 13 person forms. */
data class ConjugationTable(
    val forms: Map<ConjugationKey, List<PersonConjugation>>,
)
