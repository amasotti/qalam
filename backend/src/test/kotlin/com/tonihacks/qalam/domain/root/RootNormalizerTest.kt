package com.tonihacks.qalam.domain.root

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RootNormalizerTest : FunSpec({

    val rah = NormalizedRoot(
        letters = listOf("ر", "ح", "ب"),
        normalizedForm = "رحب",
        displayForm = "ر-ح-ب",
        letterCount = 3,
    )

    context("separator variants — all produce identical output") {
        test("space-separated") { RootNormalizer.normalize("ر ح ب") shouldBe Either.Right(rah) }
        test("dash-separated")  { RootNormalizer.normalize("ر-ح-ب") shouldBe Either.Right(rah) }
        test("comma-separated") { RootNormalizer.normalize("ر,ح,ب") shouldBe Either.Right(rah) }
        test("concatenated")    { RootNormalizer.normalize("رحب")   shouldBe Either.Right(rah) }
    }

    context("whitespace handling") {
        test("leading and trailing spaces are stripped") {
            RootNormalizer.normalize("  ر ح ب  ") shouldBe Either.Right(rah)
        }
        test("multiple spaces between letters collapse") {
            RootNormalizer.normalize("ر  ح  ب") shouldBe Either.Right(rah)
        }
    }

    context("letter count boundaries") {
        test("two-letter root is valid") {
            RootNormalizer.normalize("ر ح").let { it.isRight() shouldBe true }
        }
        test("six-letter root is valid") {
            RootNormalizer.normalize("ر ح ب ك ت ب").let { it.isRight() shouldBe true }
        }
        test("single letter is rejected") {
            RootNormalizer.normalize("ر").shouldBeInstanceOf<Either.Left<*>>()
        }
        test("seven letters are rejected") {
            RootNormalizer.normalize("ر ح ب ر ح ب ر").shouldBeInstanceOf<Either.Left<*>>()
        }
    }

    context("invalid characters") {
        test("Latin character is rejected") {
            RootNormalizer.normalize("r,ح,ب").shouldBeInstanceOf<Either.Left<*>>()
        }
        test("digit is rejected") {
            RootNormalizer.normalize("3,ح,ب").shouldBeInstanceOf<Either.Left<*>>()
        }
        test("diacritic (fathah) in separated input is rejected") {
            // "رَ" is ra + fathah diacritic — length 2, invalid single-letter slot
            RootNormalizer.normalize("رَ,ح,ب").shouldBeInstanceOf<Either.Left<*>>()
        }
        test("diacritic in concatenated input is rejected") {
            // "رَحب" — split into chars includes the diacritic U+064E, outside consonant range
            RootNormalizer.normalize("رَحب").shouldBeInstanceOf<Either.Left<*>>()
        }
    }

    context("blank input") {
        test("empty string is rejected") {
            RootNormalizer.normalize("").shouldBeInstanceOf<Either.Left<*>>()
        }
        test("whitespace-only string is rejected") {
            RootNormalizer.normalize("   ").shouldBeInstanceOf<Either.Left<*>>()
        }
    }
})
