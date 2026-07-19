package com.tonihacks.qalam.domain.conjugationexercise

import com.tonihacks.qalam.domain.conjugation.MsaConjugationEngine
import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class MatchingBoardTest : FunSpec({
    val table = MsaConjugationEngine().conjugate(
        rootLetters = listOf("ك", "ت", "ب"),
        verbForm = VerbPattern.I,
        pastPattern = "fa3ala",
        presentPattern = "yaf3ulu",
        weaknessType = WeaknessType.SOUND,
    )

    test("selects four distinct, stable present-active forms") {
        val forms = table.matchingForms(Tense.PRESENT, Voice.ACTIVE)

        forms.size shouldBe MATCHING_PAIR_COUNT
        forms.map { it.person } shouldContainExactly listOf(
            Person.FIRST_SINGULAR,
            Person.SECOND_SINGULAR_MASC,
            Person.SECOND_SINGULAR_FEM,
            Person.THIRD_SINGULAR_MASC,
        )
        forms.map { it.arabic } shouldContainExactly listOf("أَكْتُبُ", "تَكْتُبُ", "تَكْتُبِينَ", "يَكْتُبُ")
    }

    test("uses unambiguous morphology labels") {
        Person.SECOND_PLURAL_FEM.exerciseLabel() shouldBe "2nd plural feminine"
        Person.THIRD_DUAL_MASC.exerciseLabel() shouldBe "3rd dual masculine"
    }
})
