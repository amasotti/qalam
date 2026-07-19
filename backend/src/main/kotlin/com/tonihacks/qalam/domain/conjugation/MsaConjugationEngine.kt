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
import com.tonihacks.qalam.domain.conjugation.rules.FormStemBuilder
import com.tonihacks.qalam.domain.conjugation.rules.PAST_R3_GETS_SUKUN
import com.tonihacks.qalam.domain.conjugation.rules.PAST_SUFFIXES
import com.tonihacks.qalam.domain.conjugation.rules.PRESENT_PREFIXES
import com.tonihacks.qalam.domain.conjugation.rules.PRESENT_SUFFIXES_INDICATIVE
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

        val forms = mutableMapOf<ConjugationKey, List<PersonConjugation>>()

        if (verbForm == VerbPattern.I) {
            forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)] =
                conjugateFormIPast(rootLetters, pastPattern, Voice.ACTIVE)
            forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)] =
                conjugateFormIPast(rootLetters, pastPattern, Voice.PASSIVE)
            forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)] =
                conjugateFormIPresent(rootLetters, presentPattern, Voice.ACTIVE)
            forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)] =
                conjugateFormIPresent(rootLetters, presentPattern, Voice.PASSIVE)
        } else {
            val r1 = rootLetters[0]; val r2 = rootLetters[1]; val r3 = rootLetters[2]

            for (voice in Voice.entries) {
                val isPassive = voice == Voice.PASSIVE
                val stem = FormStemBuilder.build(verbForm, r1, r2, r3, isPassive)

                forms[ConjugationKey(Tense.PAST, voice)] =
                    conjugateFormIIXPast(stem, voice)
                forms[ConjugationKey(Tense.PRESENT, voice)] =
                    conjugateFormIIXPresent(stem, voice)
            }
        }

        return ConjugationTable(forms)
    }

    // ── Form I ──────────────────────────────────────────────────────────

    private fun conjugateFormIPast(
        rootLetters: List<String>,
        pastPattern: String?,
        voice: Voice,
    ): List<PersonConjugation> {
        val stem = when (voice) {
            Voice.ACTIVE -> FormIStemBuilder.buildPastActiveStem(rootLetters, pastPattern)
            Voice.PASSIVE -> FormIStemBuilder.buildPastPassiveStem(rootLetters)
        }
        return Person.entries.map { buildFormIPast(it, stem) }
    }

    private fun conjugateFormIPresent(
        rootLetters: List<String>,
        presentPattern: String?,
        voice: Voice,
    ): List<PersonConjugation> {
        val stem = when (voice) {
            Voice.ACTIVE -> FormIStemBuilder.buildPresentActiveStem(rootLetters, presentPattern)
            Voice.PASSIVE -> FormIStemBuilder.buildPresentPassiveStem(rootLetters)
        }
        return Person.entries.map { buildFormIPresent(it, stem, voice) }
    }

    private fun buildFormIPast(person: Person, stem: StemParts): PersonConjugation {
        val suffix = PAST_SUFFIXES[person]!!
        val segments = mutableListOf<Segment>()

        segments.add(Segment(stem.r1, SegmentType.ROOT))
        segments.add(Segment(stem.r1Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r2, SegmentType.ROOT))
        segments.add(Segment(stem.r2Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r3, SegmentType.ROOT))

        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        } else {
            // 3MS: R3 ends with fatḥa
            segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    private fun buildFormIPresent(person: Person, stem: StemParts, voice: Voice): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val suffix = PRESENT_SUFFIXES_INDICATIVE[person]!!
        val segments = mutableListOf<Segment>()

        val prefix = if (voice == Voice.PASSIVE) {
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            prefixTemplate
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        segments.add(Segment(stem.r1, SegmentType.ROOT))
        segments.add(Segment(stem.r1Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r2, SegmentType.ROOT))
        segments.add(Segment(stem.r2Vowel, SegmentType.PATTERN_VOWEL))
        segments.add(Segment(stem.r3, SegmentType.ROOT))

        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    // ── Forms II–X ──────────────────────────────────────────────────────

    private fun conjugateFormIIXPast(
        stem: FormStemBuilder.FormStem,
        voice: Voice,
    ): List<PersonConjugation> =
        Person.entries.map { buildFormIIXPast(it, stem, voice) }

    private fun conjugateFormIIXPresent(
        stem: FormStemBuilder.FormStem,
        voice: Voice,
    ): List<PersonConjugation> =
        Person.entries.map { buildFormIIXPresent(it, stem, voice) }

    private fun buildFormIIXPast(
        person: Person,
        stem: FormStemBuilder.FormStem,
        @Suppress("UnusedParameter") voice: Voice,
    ): PersonConjugation {
        val suffix = PAST_SUFFIXES[person]!!
        val segments = mutableListOf<Segment>()

        // Add all stem segments (includes pattern prefixes like تَ, اِسْتَ, etc.)
        segments.addAll(stem.pastSegments)

        // Suffix
        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        } else {
            // 3MS: final fatḥa
            segments.add(Segment(FATHA, SegmentType.PATTERN_VOWEL))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }

    private fun buildFormIIXPresent(
        person: Person,
        stem: FormStemBuilder.FormStem,
        voice: Voice,
    ): PersonConjugation {
        val prefixTemplate = PRESENT_PREFIXES[person]!!
        val suffix = PRESENT_SUFFIXES_INDICATIVE[person]!!
        val segments = mutableListOf<Segment>()

        // Prefix — use form-specific vowel (damma for II/III/IV, fatḥa for V-X)
        val isPassive = voice == Voice.PASSIVE
        val prefixVowel = stem.presentPrefixVowel
        val prefix = if (isPassive && prefixVowel == FATHA) {
            // Passive of forms that normally use fatḥa prefix → damma
            prefixTemplate.replace(FATHA, DAMMA)
        } else {
            // Replace default fatḥa with form-specific vowel
            prefixTemplate.replace(FATHA, prefixVowel)
        }
        segments.add(Segment(prefix, SegmentType.PREFIX))

        // Add all present stem segments
        segments.addAll(stem.presentSegments)

        // Suffix
        if (suffix.isNotEmpty()) {
            segments.add(Segment(suffix, SegmentType.SUFFIX))
        }

        val arabic = segments.joinToString("") { it.text }
        return PersonConjugation(person, arabic, segments)
    }
}
