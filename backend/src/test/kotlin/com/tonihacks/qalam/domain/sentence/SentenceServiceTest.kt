package com.tonihacks.qalam.domain.sentence

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ─── test-data factories ──────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
private fun aToken(
    position: Int = 1,
    arabic: String = "كلمة",
) = AlignmentToken(
    id = AlignmentTokenId(UUID.randomUUID()),
    sentenceId = SentenceId(UUID.randomUUID()),
    position = position,
    arabic = arabic,
    transliteration = null,
    translation = null,
    wordId = null,
)

@OptIn(ExperimentalTime::class)
private fun aSentence(
    arabicText: String = "هذه جملة",
    position: Int = 1,
    tokensValid: Boolean = true,
    tokens: List<AlignmentToken> = emptyList(),
) = Sentence(
    id = SentenceId(UUID.randomUUID()),
    textId = TextId(UUID.randomUUID()),
    position = position,
    arabicText = arabicText,
    transliteration = null,
    freeTranslation = null,
    notes = null,
    tokensValid = tokensValid,
    tokens = tokens,
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),
)

// ─── tests ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
class SentenceServiceTest : FreeSpec({

    val repo = mockk<SentenceRepository>()
    val service = SentenceService(repo)

    beforeTest { clearMocks(repo) }

    val sampleSentence = aSentence()
    val sampleId = sampleSentence.id
    val sampleTextId = sampleSentence.textId

    "listByText" - {

        "delegates to repo and returns list on success" {
            coEvery { repo.findAllByTextId(sampleTextId) } returns listOf(sampleSentence).right()

            val result = service.listByText(sampleTextId)

            result.isRight() shouldBe true
            result.getOrNull()!! shouldBe listOf(sampleSentence)
            coVerify(exactly = 1) { repo.findAllByTextId(sampleTextId) }
        }

        "propagates error from repo" {
            val err = DomainError.DatabaseError
            coEvery { repo.findAllByTextId(sampleTextId) } returns err.left()

            service.listByText(sampleTextId) shouldBe err.left()
        }
    }

    "getById" - {

        "returns sentence on success" {
            coEvery { repo.findById(sampleId) } returns sampleSentence.right()

            val result = service.getById(sampleId)

            result.isRight() shouldBe true
            result.getOrNull()!! shouldBe sampleSentence
        }

        "returns NotFound when repo returns error" {
            val err = DomainError.NotFound("Sentence", sampleId.toString())
            coEvery { repo.findById(sampleId) } returns err.left()

            service.getById(sampleId) shouldBe err.left()
        }
    }

    "create" - {

        "blank arabicText returns ValidationError without calling repo" {
            val result = service.create(textId = sampleTextId, arabicText = "   ")

            result shouldBe DomainError.ValidationError("arabicText", "arabicText must not be blank").left()
            coVerify(exactly = 0) { repo.save(any()) }
        }

        "valid input calls repo.save with correct fields" {
            coEvery { repo.save(any()) } answers { firstArg<Sentence>().right() }

            val result = service.create(
                textId = sampleTextId,
                arabicText = "هذه جملة جديدة",
                position = 3,
            )

            result.isRight() shouldBe true
            val saved = result.getOrNull()!!
            saved.textId shouldBe sampleTextId
            saved.arabicText shouldBe "هذه جملة جديدة"
            saved.tokensValid shouldBe true
            saved.tokens shouldBe emptyList()
            saved.position shouldBe 3
            coVerify(exactly = 1) { repo.save(any()) }
        }

        "when position is null, calls repo.maxPosition and uses result+1" {
            coEvery { repo.maxPosition(sampleTextId) } returns 4.right()
            coEvery { repo.save(any()) } answers { firstArg<Sentence>().right() }

            val result = service.create(textId = sampleTextId, arabicText = "جملة")

            result.isRight() shouldBe true
            result.getOrNull()!!.position shouldBe 5
            coVerify(exactly = 1) { repo.maxPosition(sampleTextId) }
        }

        "when position is provided, uses it directly without calling maxPosition" {
            coEvery { repo.save(any()) } answers { firstArg<Sentence>().right() }

            val result = service.create(textId = sampleTextId, arabicText = "جملة", position = 7)

            result.isRight() shouldBe true
            result.getOrNull()!!.position shouldBe 7
            coVerify(exactly = 0) { repo.maxPosition(any()) }
        }
    }

    "update" - {

        "returns NotFound when sentence doesn't exist" {
            val err = DomainError.NotFound("Sentence", sampleId.toString())
            coEvery { repo.findById(sampleId) } returns err.left()

            val result = service.update(id = sampleId, arabicText = "نص جديد")

            result shouldBe err.left()
        }

        "partial update: only arabicText provided — other fields unchanged" {
            val existing = aSentence(arabicText = "القديمة", tokensValid = true).copy(
                transliteration = "al-qadīma",
                freeTranslation = "the old one",
                notes = "some notes",
            )
            coEvery { repo.findById(existing.id) } returns existing.right()
            coEvery { repo.update(any()) } answers { firstArg<Sentence>().right() }

            val result = service.update(id = existing.id, arabicText = "الجديدة")

            result.isRight() shouldBe true
            val updated = result.getOrNull()!!
            updated.arabicText shouldBe "الجديدة"
            updated.transliteration shouldBe "al-qadīma"
            updated.freeTranslation shouldBe "the old one"
            updated.notes shouldBe "some notes"
        }

        "changing arabicText sets tokensValid to false" {
            val existing = aSentence(arabicText = "النص القديم", tokensValid = true)
            coEvery { repo.findById(existing.id) } returns existing.right()
            coEvery { repo.update(any()) } answers { firstArg<Sentence>().right() }

            val result = service.update(id = existing.id, arabicText = "النص الجديد")

            result.isRight() shouldBe true
            result.getOrNull()!!.tokensValid shouldBe false
        }

        "arabicText provided but same value — tokensValid stays true" {
            val existing = aSentence(arabicText = "نفس النص", tokensValid = true)
            coEvery { repo.findById(existing.id) } returns existing.right()
            coEvery { repo.update(any()) } answers { firstArg<Sentence>().right() }

            val result = service.update(id = existing.id, arabicText = "نفس النص")

            result.isRight() shouldBe true
            result.getOrNull()!!.tokensValid shouldBe true
        }

        "transliteration updated, arabicText unchanged — tokensValid stays true" {
            val existing = aSentence(arabicText = "النص", tokensValid = true)
            coEvery { repo.findById(existing.id) } returns existing.right()
            coEvery { repo.update(any()) } answers { firstArg<Sentence>().right() }

            val result = service.update(id = existing.id, transliteration = "an-nass")

            result.isRight() shouldBe true
            result.getOrNull()!!.tokensValid shouldBe true
            result.getOrNull()!!.transliteration shouldBe "an-nass"
        }
    }

    "delete" - {

        "calls repo.delete and returns Unit on success" {
            coEvery { repo.delete(sampleId) } returns Unit.right()

            service.delete(sampleId) shouldBe Unit.right()
            coVerify(exactly = 1) { repo.delete(sampleId) }
        }

        "propagates NotFound from repo" {
            val err = DomainError.NotFound("Sentence", sampleId.toString())
            coEvery { repo.delete(sampleId) } returns err.left()

            service.delete(sampleId) shouldBe err.left()
        }
    }

    "replaceTokens" - {

        "blank arabic in any TokenInput returns ValidationError without calling repo" {
            val inputs = listOf(
                TokenInput(position = 1, arabic = "كلمة", transliteration = null, translation = null, wordId = null),
                TokenInput(position = 2, arabic = "  ", transliteration = null, translation = null, wordId = null),
            )

            val result = service.replaceTokens(sampleId, inputs)

            result shouldBe DomainError.ValidationError("arabic", "Token at position 2 must not have blank arabic").left()
            coVerify(exactly = 0) { repo.replaceTokens(any(), any()) }
        }

        "valid inputs: calls repo.replaceTokens with correctly mapped AlignmentToken list" {
            val inputs = listOf(
                TokenInput(position = 1, arabic = "هذه", transliteration = "hādhihi", translation = "this", wordId = null),
                TokenInput(position = 2, arabic = "جملة", transliteration = null, translation = "sentence", wordId = null),
            )
            coEvery { repo.findById(sampleId) } returns sampleSentence.right()
            coEvery { repo.replaceTokens(sampleId, any()) } returns sampleSentence.right()

            val result = service.replaceTokens(sampleId, inputs)

            result.isRight() shouldBe true
            coVerify(exactly = 1) {
                repo.replaceTokens(sampleId, match { tokens ->
                    tokens.size == 2 &&
                        tokens[0].arabic == "هذه" &&
                        tokens[0].position == 1 &&
                        tokens[0].transliteration == "hādhihi" &&
                        tokens[0].sentenceId == sampleSentence.id &&
                        tokens[1].arabic == "جملة" &&
                        tokens[1].position == 2
                })
            }
        }

        "propagates NotFound from repo when sentence doesn't exist" {
            val err = DomainError.NotFound("Sentence", sampleId.toString())
            val inputs = listOf(
                TokenInput(position = 1, arabic = "كلمة", transliteration = null, translation = null, wordId = null),
            )
            coEvery { repo.findById(sampleId) } returns err.left()

            service.replaceTokens(sampleId, inputs) shouldBe err.left()
        }
    }

    "clearTokens" - {

        "calls repo.replaceTokens with emptyList" {
            coEvery { repo.replaceTokens(sampleId, emptyList()) } returns sampleSentence.right()

            val result = service.clearTokens(sampleId)

            result.isRight() shouldBe true
            coVerify(exactly = 1) { repo.replaceTokens(sampleId, emptyList()) }
        }
    }
})
