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

    // ── Forms II–X ──────────────────────────────────────────────────────

    context("Form II — درّس (d-r-s, to teach)") {
        val rootDRS = listOf("د", "ر", "س")
        val table = engine.conjugate(rootDRS, VerbPattern.II, null, null, WeaknessType.SOUND)

        context("past active") {
            val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
            fun formFor(p: Person) = past.first { it.person == p }.arabic

            test("3MS — دَرَّسَ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "دَرَّسَ" }
            test("1S — دَرَّسْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "دَرَّسْتُ" }
            test("3FS — دَرَّسَتْ") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "دَرَّسَتْ" }
            test("3PM — دَرَّسُوا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "دَرَّسُوا" }
            test("2SF — دَرَّسْتِ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "دَرَّسْتِ" }
        }

        context("present active") {
            val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            fun formFor(p: Person) = pres.first { it.person == p }.arabic

            test("3SM — يُدَرِّسُ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يُدَرِّسُ" }
            test("1S — أُدَرِّسُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أُدَرِّسُ" }
            test("3PM — يُدَرِّسُونَ") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يُدَرِّسُونَ" }
        }

        context("passive") {
            test("past passive 3MS — دُرِّسَ") {
                val pastP = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
                pastP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "دُرِّسَ"
            }

            test("present passive 3SM — يُدَرَّسُ") {
                val presP = table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)]!!
                presP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يُدَرَّسُ"
            }
        }
    }

    context("Form V — تعلّم (3-l-m, to learn)") {
        val rootALM = listOf("ع", "ل", "م")
        val table = engine.conjugate(rootALM, VerbPattern.V, null, null, WeaknessType.SOUND)

        context("past active") {
            val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
            fun formFor(p: Person) = past.first { it.person == p }.arabic

            test("3MS — تَعَلَّمَ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "تَعَلَّمَ" }
            test("1S — تَعَلَّمْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "تَعَلَّمْتُ" }
            test("3PM — تَعَلَّمُوا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "تَعَلَّمُوا" }
        }

        context("present active") {
            val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            fun formFor(p: Person) = pres.first { it.person == p }.arabic

            test("3SM — يَتَعَلَّمُ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَتَعَلَّمُ" }
            test("1S — أَتَعَلَّمُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أَتَعَلَّمُ" }
        }
    }

    context("Form X — استخدم (خ-د-م, to use)") {
        val rootKhDM = listOf("خ", "د", "م")
        val table = engine.conjugate(rootKhDM, VerbPattern.X, null, null, WeaknessType.SOUND)

        context("past active") {
            val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
            fun formFor(p: Person) = past.first { it.person == p }.arabic

            test("3MS — اِسْتَخْدَمَ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "ا${'\u0650'}سْتَخْدَمَ" }
            test("1S — اِسْتَخْدَمْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "ا${'\u0650'}سْتَخْدَمْتُ" }
            test("3PM — اِسْتَخْدَمُوا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "ا${'\u0650'}سْتَخْدَمُوا" }
        }

        context("present active") {
            val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            fun formFor(p: Person) = pres.first { it.person == p }.arabic

            test("3SM — يَسْتَخْدِمُ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَسْتَخْدِمُ" }
            test("1S — أَسْتَخْدِمُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أَسْتَخْدِمُ" }
        }

        context("passive") {
            test("past passive 3MS — اُسْتُخْدِمَ") {
                val pastP = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
                pastP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "ا${'\u064F'}سْتُخْدِمَ"
            }

            test("present passive 3SM — يُسْتَخْدَمُ") {
                val presP = table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)]!!
                presP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يُسْتَخْدَمُ"
            }
        }
    }

    context("Form IV — أفعل (أرسل, to send)") {
        val rootRSL = listOf("ر", "س", "ل")
        val table = engine.conjugate(rootRSL, VerbPattern.IV, null, null, WeaknessType.SOUND)

        test("past active 3MS — أَرْسَلَ") {
            val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
            past.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "أَرْسَلَ"
        }

        test("present active 3SM — يُرْسِلُ") {
            val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
            pres.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يُرْسِلُ"
        }
    }

    // ── Weak Verbs ───────────────────────────────────────────────────────

    // قال (q-w-l) — hollow verb, R2 = و
    context("Hollow verb قال (q-w-l) — past active") {
        val rootQWL = listOf("ق", "و", "ل")
        val table = engine.conjugate(rootQWL, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.HOLLOW)
        val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!

        fun formFor(p: Person) = past.first { it.person == p }.arabic

        test("3MS — قَالَ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "قَالَ" }
        test("3FS — قَالَتْ") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "قَالَتْ" }
        test("1S — قُلْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "قُلْتُ" }
        test("2SM — قُلْتَ") { formFor(Person.SECOND_SINGULAR_MASC) shouldBe "قُلْتَ" }
        test("2SF — قُلْتِ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "قُلْتِ" }
        test("3DM — قَالَا") { formFor(Person.THIRD_DUAL_MASC) shouldBe "قَالَا" }
        test("3PM — قَالُوا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "قَالُوا" }
        test("1P — قُلْنَا") { formFor(Person.FIRST_PLURAL) shouldBe "قُلْنَا" }
        test("3PF — قُلْنَ") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "قُلْنَ" }
    }

    context("Hollow verb قال (q-w-l) — present active") {
        val rootQWL = listOf("ق", "و", "ل")
        val table = engine.conjugate(rootQWL, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.HOLLOW)
        val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!

        fun formFor(p: Person) = pres.first { it.person == p }.arabic

        test("3SM — يَقُولُ") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَقُولُ" }
        test("1S — أَقُولُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أَقُولُ" }
        test("2SF — تَقُولِينَ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "تَقُولِينَ" }
        test("3PM — يَقُولُونَ") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يَقُولُونَ" }
        test("3PF contracted — يَقُلْنَ") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "يَقُلْنَ" }
    }

    context("Hollow verb قال — passive") {
        val rootQWL = listOf("ق", "و", "ل")
        val table = engine.conjugate(rootQWL, VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.HOLLOW)

        test("past passive 3MS — قِيلَ") {
            val pastP = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
            pastP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "قِيلَ"
        }

        test("past passive 1S — قِلْتُ") {
            val pastP = table.forms[ConjugationKey(Tense.PAST, Voice.PASSIVE)]!!
            pastP.first { it.person == Person.FIRST_SINGULAR }.arabic shouldBe "قِلْتُ"
        }

        test("present passive 3SM — يُقَالُ") {
            val presP = table.forms[ConjugationKey(Tense.PRESENT, Voice.PASSIVE)]!!
            presP.first { it.person == Person.THIRD_SINGULAR_MASC }.arabic shouldBe "يُقَالُ"
        }
    }

    // مشى (m-sh-y) — defective verb, R3 = ي
    context("Defective verb مشى (m-sh-y) — past active") {
        val rootMShY = listOf("م", "ش", "ي")
        val table = engine.conjugate(rootMShY, VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.DEFECTIVE)
        val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!

        fun formFor(p: Person) = past.first { it.person == p }.arabic

        test("3MS — مَشَى") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "مَشَى" }
        test("3FS — مَشَتْ") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "مَشَتْ" }
        test("1S — مَشَيْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "مَشَيْتُ" }
        test("3DM — مَشَيَا") { formFor(Person.THIRD_DUAL_MASC) shouldBe "مَشَيَا" }
        test("3PM — مَشَوْا") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "مَشَوْا" }
        test("3PF — مَشَيْنَ") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "مَشَيْنَ" }
    }

    context("Defective verb مشى (m-sh-y) — present active") {
        val rootMShY = listOf("م", "ش", "ي")
        val table = engine.conjugate(rootMShY, VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.DEFECTIVE)
        val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!

        fun formFor(p: Person) = pres.first { it.person == p }.arabic

        test("3SM — يَمْشِي") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَمْشِي" }
        test("1S — أَمْشِي") { formFor(Person.FIRST_SINGULAR) shouldBe "أَمْشِي" }
        test("3PM — يَمْشُونَ") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يَمْشُونَ" }
        test("2SF — تَمْشِينَ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "تَمْشِينَ" }
        test("3DM — تَمْشِيَانِ") {
            // Note: 3DM uses يَ prefix
            val form = pres.first { it.person == Person.THIRD_DUAL_MASC }
            form.arabic shouldBe "يَمْشِيَانِ"
        }
    }

    // وصل (w-s-l) — assimilated verb, R1 = و
    context("Assimilated verb وصل (w-s-l) — past active (regular)") {
        val rootWSL = listOf("و", "ص", "ل")
        val table = engine.conjugate(rootWSL, VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.ASSIMILATED)
        val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!

        fun formFor(p: Person) = past.first { it.person == p }.arabic

        test("3MS — وَصَلَ (regular past)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "وَصَلَ" }
        test("1S — وَصَلْتُ") { formFor(Person.FIRST_SINGULAR) shouldBe "وَصَلْتُ" }
    }

    context("Assimilated verb وصل (w-s-l) — present active (R1 drops)") {
        val rootWSL = listOf("و", "ص", "ل")
        val table = engine.conjugate(rootWSL, VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.ASSIMILATED)
        val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!

        fun formFor(p: Person) = pres.first { it.person == p }.arabic

        test("3SM — يَصِلُ (R1 wāw dropped)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَصِلُ" }
        test("1S — أَصِلُ") { formFor(Person.FIRST_SINGULAR) shouldBe "أَصِلُ" }
        test("3PM — يَصِلُونَ") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يَصِلُونَ" }
        test("2SF — تَصِلِينَ") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "تَصِلِينَ" }
    }

    context("Weak verb tables completeness") {
        test("hollow verb produces 4 tables × 13 persons") {
            val table = engine.conjugate(listOf("ق", "و", "ل"), VerbPattern.I, "fa3ala", "yaf3ulu", WeaknessType.HOLLOW)
            table.forms.size shouldBe 4
            table.forms.values.forEach { it.size shouldBe 13 }
        }

        test("defective verb produces 4 tables × 13 persons") {
            val table = engine.conjugate(listOf("م", "ش", "ي"), VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.DEFECTIVE)
            table.forms.size shouldBe 4
            table.forms.values.forEach { it.size shouldBe 13 }
        }

        test("assimilated verb produces 4 tables × 13 persons") {
            val table = engine.conjugate(listOf("و", "ص", "ل"), VerbPattern.I, "fa3ala", "yaf3ilu", WeaknessType.ASSIMILATED)
            table.forms.size shouldBe 4
            table.forms.values.forEach { it.size shouldBe 13 }
        }
    }

    // ── Geminate verbs (R2 = R3) ─────────────────────────────────────────

    // قَرَّ (q-r-r) — Form I, "to settle / be steady", fa3ala / yaf3iru
    context("Geminate Form I قَرَّ (q-r-r) — past active") {
        val rootQRR = listOf("ق", "ر", "ر")
        val table = engine.conjugate(rootQRR, VerbPattern.I, "fa3ala", "yaf3iru", WeaknessType.GEMINATE)
        val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
        fun formFor(p: Person) = past.first { it.person == p }.arabic

        // Vowel-initial suffix → contracted: قَرَّ / قَرَّتْ / قَرُّوا
        test("3SM — قَرَّ (contracted)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "قَرَّ" }
        test("3SF — قَرَّتْ (contracted)") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "قَرَّتْ" }
        test("3DM — قَرَّا (contracted)") { formFor(Person.THIRD_DUAL_MASC) shouldBe "قَرَّا" }
        test("3DF — قَرَّتَا (contracted)") { formFor(Person.THIRD_DUAL_FEM) shouldBe "قَرَّتَا" }
        test("3PM — قَرُّوا (contracted, damma)") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "قَرُّوا" }
        // Consonant-initial suffix → uncontracted: قَرَرْتُ / قَرَرْنَ
        test("1S — قَرَرْتُ (uncontracted)") { formFor(Person.FIRST_SINGULAR) shouldBe "قَرَرْتُ" }
        test("2SM — قَرَرْتَ (uncontracted)") { formFor(Person.SECOND_SINGULAR_MASC) shouldBe "قَرَرْتَ" }
        test("1P — قَرَرْنَا (uncontracted)") { formFor(Person.FIRST_PLURAL) shouldBe "قَرَرْنَا" }
        test("3PF — قَرَرْنَ (uncontracted)") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "قَرَرْنَ" }
    }

    context("Geminate Form I قَرَّ (q-r-r) — present active") {
        val rootQRR = listOf("ق", "ر", "ر")
        val table = engine.conjugate(rootQRR, VerbPattern.I, "fa3ala", "yaf3iru", WeaknessType.GEMINATE)
        val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
        fun formFor(p: Person) = pres.first { it.person == p }.arabic

        // Vowel-initial suffix → contracted; R1 takes R2's kasra, R2 gets shadda + suffix vowel
        test("1S — أَقِرُّ (contracted)") { formFor(Person.FIRST_SINGULAR) shouldBe "أَقِرُّ" }
        test("3SM — يَقِرُّ (contracted)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَقِرُّ" }
        test("2SF — تَقِرِّينَ (contracted)") { formFor(Person.SECOND_SINGULAR_FEM) shouldBe "تَقِرِّينَ" }
        test("3PM — يَقِرُّونَ (contracted)") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "يَقِرُّونَ" }
        // Sukūn-initial suffix → uncontracted
        test("2PF — تَقْرِرْنَ (uncontracted)") { formFor(Person.SECOND_PLURAL_FEM) shouldBe "تَقْرِرْنَ" }
        test("3PF — يَقْرِرْنَ (uncontracted)") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "يَقْرِرْنَ" }
    }

    // اِسْتَقَرَّ (q-r-r, Form X) — "to settle / stabilize"
    context("Geminate Form X اِسْتَقَرَّ (q-r-r) — past active") {
        val rootQRR = listOf("ق", "ر", "ر")
        val table = engine.conjugate(rootQRR, VerbPattern.X, null, null, WeaknessType.GEMINATE)
        val past = table.forms[ConjugationKey(Tense.PAST, Voice.ACTIVE)]!!
        fun formFor(p: Person) = past.first { it.person == p }.arabic

        test("3SM — اِسْتَقَرَّ (contracted)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "ا\u0650سْتَقَرَّ" }
        test("3SF — اِسْتَقَرَّتْ (contracted)") { formFor(Person.THIRD_SINGULAR_FEM) shouldBe "ا\u0650سْتَقَرَّتْ" }
        test("1S — اِسْتَقْرَرْتُ (uncontracted)") { formFor(Person.FIRST_SINGULAR) shouldBe "ا\u0650سْتَقْرَرْتُ" }
        test("3PM — اِسْتَقَرُّوا (contracted)") { formFor(Person.THIRD_PLURAL_MASC) shouldBe "ا\u0650سْتَقَرُّوا" }
    }

    context("Geminate Form X اِسْتَقَرَّ (q-r-r) — present active") {
        val rootQRR = listOf("ق", "ر", "ر")
        val table = engine.conjugate(rootQRR, VerbPattern.X, null, null, WeaknessType.GEMINATE)
        val pres = table.forms[ConjugationKey(Tense.PRESENT, Voice.ACTIVE)]!!
        fun formFor(p: Person) = pres.first { it.person == p }.arabic

        test("1S — أَسْتَقِرُّ (contracted)") { formFor(Person.FIRST_SINGULAR) shouldBe "أَسْتَقِرُّ" }
        test("3SM — يَسْتَقِرُّ (contracted)") { formFor(Person.THIRD_SINGULAR_MASC) shouldBe "يَسْتَقِرُّ" }
        test("3PF — يَسْتَقْرِرْنَ (uncontracted)") { formFor(Person.THIRD_PLURAL_FEM) shouldBe "يَسْتَقْرِرْنَ" }
    }

    context("Geminate verb produces 4 tables × 13 persons") {
        test("Form I geminate") {
            val table = engine.conjugate(listOf("ق", "ر", "ر"), VerbPattern.I, "fa3ala", "yaf3iru", WeaknessType.GEMINATE)
            table.forms.size shouldBe 4
            table.forms.values.forEach { it.size shouldBe 13 }
        }
        test("Form X geminate") {
            val table = engine.conjugate(listOf("ق", "ر", "ر"), VerbPattern.X, null, null, WeaknessType.GEMINATE)
            table.forms.size shouldBe 4
            table.forms.values.forEach { it.size shouldBe 13 }
        }
    }

    context("all forms produce 4 tables × 13 persons") {
        val root = listOf("ف", "ع", "ل")
        for (form in VerbPattern.entries) {
            test("Form ${form.name} produces complete table") {
                val table = engine.conjugate(root, form, "fa3ala", "yaf3ulu", WeaknessType.SOUND)
                table.forms.size shouldBe 4
                table.forms.values.forEach { it.size shouldBe 13 }
            }
        }
    }
})
