package com.tonihacks.qalam.domain.training

import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordProgress
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class MasteryPromotionTest : FreeSpec({

    fun progress(
        consecutiveCorrect: Int = 0,
        totalCorrect: Int = 0,
        totalAttempts: Int = 0,
    ) = WordProgress(
        wordId             = WordId(UUID.randomUUID()),
        consecutiveCorrect = consecutiveCorrect,
        totalAttempts      = totalAttempts,
        totalCorrect       = totalCorrect,
        lastReviewedAt     = null,
    )

    "NEW → LEARNING after 3 consecutive correct" {
        val p = progress(consecutiveCorrect = 2, totalCorrect = 2)
        val (updated, promotion) = computeProgressUpdate(p, MasteryLevel.NEW, TrainingResult.CORRECT)
        promotion shouldBe MasteryLevel.LEARNING
        updated.consecutiveCorrect shouldBe 3
        updated.totalCorrect shouldBe 3
    }

    "NEW stays NEW with only 2 consecutive correct" {
        val p = progress(consecutiveCorrect = 1, totalCorrect = 1)
        val (_, promotion) = computeProgressUpdate(p, MasteryLevel.NEW, TrainingResult.CORRECT)
        promotion.shouldBeNull()
    }

    "INCORRECT resets streak but does not demote" {
        val p = progress(consecutiveCorrect = 5, totalCorrect = 5, totalAttempts = 5)
        val (updated, promotion) = computeProgressUpdate(p, MasteryLevel.NEW, TrainingResult.INCORRECT)
        updated.consecutiveCorrect shouldBe 0
        updated.totalAttempts shouldBe 6
        updated.totalCorrect shouldBe 5
        promotion.shouldBeNull()
    }

    "LEARNING → KNOWN at 10 total correct" {
        val p = progress(consecutiveCorrect = 1, totalCorrect = 9, totalAttempts = 9)
        val (_, promotion) = computeProgressUpdate(p, MasteryLevel.LEARNING, TrainingResult.CORRECT)
        promotion shouldBe MasteryLevel.KNOWN
    }

    "LEARNING stays LEARNING at 9 total correct" {
        val p = progress(consecutiveCorrect = 1, totalCorrect = 8, totalAttempts = 8)
        val (_, promotion) = computeProgressUpdate(p, MasteryLevel.LEARNING, TrainingResult.CORRECT)
        promotion.shouldBeNull()
    }

    "KNOWN → MASTERED at 15 total correct" {
        val p = progress(consecutiveCorrect = 1, totalCorrect = 14, totalAttempts = 14)
        val (_, promotion) = computeProgressUpdate(p, MasteryLevel.KNOWN, TrainingResult.CORRECT)
        promotion shouldBe MasteryLevel.MASTERED
    }

    "MASTERED stays MASTERED — no further promotion" {
        val p = progress(consecutiveCorrect = 20, totalCorrect = 20, totalAttempts = 20)
        val (_, promotion) = computeProgressUpdate(p, MasteryLevel.MASTERED, TrainingResult.CORRECT)
        promotion.shouldBeNull()
    }

    "SKIPPED increments attempts but not correct or streak" {
        val p = progress(consecutiveCorrect = 2, totalCorrect = 2, totalAttempts = 2)
        val (updated, promotion) = computeProgressUpdate(p, MasteryLevel.NEW, TrainingResult.SKIPPED)
        updated.consecutiveCorrect shouldBe 2
        updated.totalCorrect shouldBe 2
        updated.totalAttempts shouldBe 3
        promotion.shouldBeNull()
    }
})
