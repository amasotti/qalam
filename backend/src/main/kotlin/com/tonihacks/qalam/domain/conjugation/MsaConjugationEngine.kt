package com.tonihacks.qalam.domain.conjugation

import com.tonihacks.qalam.domain.conjugation.model.ConjugationKey
import com.tonihacks.qalam.domain.conjugation.model.ConjugationTable
import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.PersonConjugation
import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugation.model.SegmentType
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.conjugation.rules.DAMMA
import com.tonihacks.qalam.domain.conjugation.rules.FATHA
import com.tonihacks.qalam.domain.conjugation.rules.FormIStemBuilder
import com.tonihacks.qalam.domain.conjugation.rules.FormIStemBuilder.StemParts
import com.tonihacks.qalam.domain.conjugation.rules.PAST_R3_GETS_SUKUN
import com.tonihacks.qalam.domain.conjugation.rules.PAST_SUFFIXES
import com.tonihacks.qalam.domain.conjugation.rules.PRESENT_PREFIXES
import com.tonihacks.qalam.domain.conjugation.rules.PRESENT_SUFFIXES_INDICATIVE
import com.tonihacks.qalam.domain.conjugation.rules.SUKUN
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType

class MsaConjugationEngine : ConjugationEngine {

    override fun conjugate(
        rootLetters: List<String>,
        verbForm: VerbPattern,
        pastPattern: String?,
        presentPattern: String?,
        weaknessType: WeaknessType,
    ): ConjugationTable {
        require(rootLetters.size >= 3) { "Root must have at least 3 letters" }

        // For now: only Form I sound verbs. Forms II-X and weak verbs in later slices.
        val forms = mutableMapOf<ConjugationKey, List<PersonConjugation>>()

        forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)] =
            conjugatePast(rootLetters, pastPattern, Voice.ACTIVE)
        forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)] =
            conjugatePast(rootLetters, pastPattern, Voice.PASSIVE)
        forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)] =
            conjugatePresent(rootLetters, presentPattern, Voice.ACTIVE)
        forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)] =
            conjugatePresent(rootLetters, presentPattern, Voice.PASSIVE)

        return ConjugationTable(forms)
    }

    private fun conjugatePast(
        rootLetters: List<String>,
        pastPattern: String?,
        voice: Voice,
    ): List<PersonConjugation> {
        val stem = when (voice) {
            Voice.ACTIVE -> FormIStemBuilder.buildPastActiveStem(rootLetters, pastPattern)
            Voice.PASSIVE -> FormIStemBuilder.buildPastPassiveStem(rootLetters)
        }

        return Person.entries.map { person ->
            buildPastForm(person, stem)
        }
    }

    private fun conjugatePresent(
        rootLetters: List<String>,
        presentPattern: String?,
        voice: Voice,
    ): List<PersonConjugation> {
        val stem = when (voice) {
            Voice.ACTIVE -> FormIStemBuilder.buildPresentActiveStem(rootLetters, presentPattern)
            Voice.PASSIVE -> FormIStemBuilder.buildPresentPassiveStem(rootLetters)
        }

        return Person.entries.map { person ->
            buildPresentForm(person, stem, voice)
        }
    }

    private fun buildPastForm(person: Person, stem: StemParts): PersonConjugation {
        val suffix = PAST_SUFFIXES[person]!!
        val segments = mutableListOf<Segment>()

        // R1 + vowel
        segments.add(Segment(stem.r1, SegmentType.ROOT))
        segments.add(Segment(stem.r1Vowel, SegmentType.PATTERN_VOWEL))

        // R2 + vowel
        segments.add(Segment(stem.r2, SegmentType.ROOT))
        segments.add(Segment(stem.r2Vowel, SegmentType.PATTERN_VOWEL))

        // R3 — vowel depends on person
        segments.add(Segment(stem.r3, SegmentType.ROOT))

        if (person in PAST_R3_GETS_SUKUN) {
            // Suffix already starts with sukūn, R3 is bare
            // The suffix string includes the sukūn
        } else {
            // R3 gets a vowel: 3MS=fatḥa, 3SF/3DF=fatḥa(in suffix), 3DM=fatḥa(in suffix), 3PM=damma(in suffix)
            // These are already embedded in the suffix strings
        }

        // Suffix
        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        } else {
            // 3MS: R3 ends with fatḥa
            segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    private fun buildPresentForm(person: Person, stem: StemParts, voice: Voice): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val suffix = PRESENT_SUFFIXES_INDICATIVE[person]!!
        val segments = mutableListOf<Segment>()

        // Prefix — in passive voice, prefix vowel changes to damma
        val prefix = if (voice == Voice.PASSIVE) {
            // Replace fatḥa in prefix with damma: يَ → يُ
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            prefixTemplate
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        // Stem: R1 + sukūn + R2 + vowel + R3
        segments.add(Segment(stem.r1, SegmentType.ROOT))
        segments.add(Segment(stem.r1Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r2, SegmentType.ROOT))
        segments.add(Segment(stem.r2Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r3, SegmentType.ROOT))

        // Suffix
        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }
}
