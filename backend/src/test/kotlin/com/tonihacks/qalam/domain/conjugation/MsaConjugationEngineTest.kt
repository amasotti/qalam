package com.tonihacks.qalam.domain.conjugation

import com.tonihacks.qalam.domain.conjugation.model.ConjugationKey
import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.SegmentType
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MsaConjugationEngineTest : FunSpec({

    val engine = MsaConjugationEngine()

    // كتب — Form I, fa3ala / yaf3ulu, sound verb
    val rootKTB = listOf("ك", "ت", "ب")

    context("Form I sound verb كتب — past active") {
        val table = engine.conjugate(rootKTB, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.SOUND)
        val pastActive = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!

        fun formFor(person: Person) = pastActive.first { it.person == person }.arabic

        test("3MS — كَتَبَ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "كَتَبَ" }
        test("3FS — كَتَبَتْ") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "كَتَبَتْ" }
        test("1S — كَتَبْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "كَتَبْتُ" }
        test("2SM — كَتَبْتَ") { formFor(Person.SECOND_SINGULAR_MASC) shouldBe "كَتَبْتَ" }
        test("2SF — كَتَبْتِ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "كَتَبْتِ" }
        test("2D — كَتَبْتُمَا") { formFor(Person.SECOND_DUAL) shouldBe "كَتَبْتُمَا" }
        test("3DM — كَتَبَا") { formFor(Person.THIRD_DUAL_MASC) shouldBe "كَتَبَا" }
        test("3DF — كَتَبَتَا") { formFor(Person.THIRD_DUAL_FEM) shouldBe "كَتَبَتَا" }
        test("1P — كَتَبْنَا") { formFor(Person.FIRST_PLURAL) shouldBe "كَتَبْنَا" }
        test("2PM — كَتَبْتُمْ") { formFor(Person.SECOND_PLURAL_MASC) shouldBe "كَتَبْتُمْ" }
        test("2PF — كَتَبْتُنَّ") { formFor(Person.SECOND_PLURAL_FEM) shouldBe "كَتَبْتُنَّ" }
        test("3PM — كَتَبُوا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "كَتَبُوا" }
        test("3PF — كَتَبْنَ") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "كَتَبْنَ" }
    }

    context("Form I sound verb كتب — present active") {
        val table = engine.conjugate(rootKTB, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.SOUND)
        val presentActive = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!

        fun formFor(person: Person) = presentActive.first { it.person == person }.arabic

        test("1S — أَكْتُبُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أَكْتُبُ" }
        test("2SM — تَكْتُبُ") { formFor(Person.SECOND_SINGULAR_MASC) shouldBe "تَكْتُبُ" }
        test("2SF — تَكْتُبِينَ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "تَكْتُبِينَ" }
        test("3SM — يَكْتُبُ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَكْتُبُ" }
        test("3SF — تَكْتُبُ") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "تَكْتُبُ" }
        test("2D — تَكْتُبَانِ") { formFor(Person.SECOND_DUAL) shouldBe "تَكْتُبَانِ" }
        test("3DM — يَكْتُبَانِ") { formFor(Person.THIRD_DUAL_MASC) shouldBe "يَكْتُبَانِ" }
        test("3DF — تَكْتُبَانِ") { formFor(Person.THIRD_DUAL_FEM) shouldBe "تَكْتُبَانِ" }
        test("1P — نَكْتُبُ") { formFor(Person.FIRST_PLURAL) shouldBe "نَكْتُبُ" }
        test("2PM — تَكْتُبُونَ") { formFor(Person.SECOND_PLURAL_MASC) shouldBe "تَكْتُبُونَ" }
        test("2PF — تَكْتُبْنَ") { formFor(Person.SECOND_PLURAL_FEM) shouldBe "تَكْتُبْنَ" }
        test("3PM — يَكْتُبُونَ") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يَكْتُبُونَ" }
        test("3PF — يَكْتُبْنَ") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "يَكْتُبْنَ" }
    }

    context("Form I sound verb كتب — passive voice") {
        val table = engine.conjugate(rootKTB, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.SOUND)

        test("past passive 3MS — كُتِبَ") {
            val pastPassive = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
            pastPassive.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "كُتِبَ"
        }

        test("past passive 1S — كُتِبْتُ") {
            val pastPassive = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
            pastPassive.first { it.person == Person.FIRST_SINGULAR }.arabic shouldBe "كُتِبْتُ"
        }

        test("present passive 3SM — يُكْتَبُ") {
            val presentPassive = table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)]!!
            presentPassive.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يُكْتَبُ"
        }

        test("present passive 1S — أُكْتَبُ") {
            val presentPassive = table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)]!!
            presentPassive.first { it.person == Person.FIRST_SINGULAR }.arabic shouldBe "أُكْتَبُ"
        }
    }

    context("segment types") {
        val table = engine.conjugate(rootKTB, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.SOUND)

        test("present 3SM يَكْتُبُ segments contain all 4 types") {
            val presentActive = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            val form = presentActive.first { it.person == Person.THIRD_SINGULAR_MASC }
            val types = form.segments.map { it.type }.toSet()
            types shouldBe setOf(SegmentType.PREFIX, SegmentType.ROOT, SegmentType.PATTERN_VOWEL, SegmentType.SUFFIX)
        }

        test("present 3SM prefix is يَ") {
            val presentActive = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            val form = presentActive.first { it.person == Person.THIRD_SINGULAR_MASC }
            form.segments.first { it.type == SegmentType.PREFIX }.text shouldBe "يَ"
        }

        test("root segments are ك ت ب") {
            val presentActive = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            val form = presentActive.first { it.person == Person.THIRD_SINGULAR_MASC }
            val roots = form.segments.filter { it.type == SegmentType.ROOT }.map { it.text }
            roots shouldBe listOf("ك", "ت", "ب")
        }
    }

    context("table completeness") {
        val table = engine.conjugate(rootKTB, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.SOUND)

        test("has all 4 tense/voice combinations") {
            table.forms.size shouldBe 4
            table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)] shouldNotBe null
            table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)] shouldNotBe null
            table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)] shouldNotBe null
            table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)] shouldNotBe null
        }

        test("each sub-table has 13 person forms") {
            table.forms.values.forEach { forms ->
                forms.size shouldBe 13
            }
        }
    }

    context("Form I with different vowel patterns") {
        // فهم — fa3ima / yaf3amu (i/a pattern)
        val rootFHM = listOf("ف", "ه", "م")

        test("past active 3MS — فَهِمَ") {
            val table = engine.conjugate(rootFHM, VerbPattern.I, "fa3ima", "yaf3amu", WeaknessType.SOUND)
            val pastActive = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
            pastActive.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "فَهِمَ"
        }

        test("present active 3SM — يَفْهَمُ") {
            val table = engine.conjugate(rootFHM, VerbPattern.I, "fa3ima", "yaf3amu", WeaknessType.SOUND)
            val presentActive = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            presentActive.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يَفْهَمُ"
        }
    }
})
