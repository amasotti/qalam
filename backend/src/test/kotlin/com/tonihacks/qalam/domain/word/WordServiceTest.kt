package com.tonihacks.qalam.domain.word

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.word.CreateDictionaryLinkRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordRequest
import com.tonihacks.qalam.delivery.dto.word.UpdateWordRequest
import com.tonihacks.qalam.delivery.dto.word.UpsertVerbDetailsRequest
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.infrastructure.ai.OpenRouterWordClient
import com.tonihacks.qalam.infrastructure.exposed.ExposedVerbDetailsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Instant

class WordServiceTest : FunSpec({

    val repo = mockk<WordRepository>()
    val aiClient = mockk<OpenRouterWordClient>()
    val verbDetailsRepo = mockk<ExposedVerbDetailsRepository>()
    val service = WordService(repo, aiClient, verbDetailsRepo)

    beforeTest { clearMocks(repo, verbDetailsRepo) }

    val sampleId = UUID.randomUUID()
    val sampleLinkId = UUID.randomUUID()
    val sampleWord = Word(
        id = WordId(sampleId),
        arabicText = "كَتَبَ",
        transliteration = "kataba",
        translation = "to write",
        partOfSpeech = PartOfSpeech.VERB,
        dialect = Dialect.MSA,
        difficulty = Difficulty.BEGINNER,
        masteryLevel = MasteryLevel.NEW,
        pronunciationUrl = null,
        rootId = null,
        derivedFromId = null,
        notes = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
    val sampleLink = DictionaryLink(
        id = DictionaryLinkId(sampleLinkId),
        wordId = WordId(sampleId),
        source = DictionarySource.ALMANY,
        url = "https://www.almany.de/",
    )

    context("create") {
        test("happy path — creates word with NEW mastery level") {
            coEvery { repo.create(any()) } answers { firstArg<Word>().right() }

            val result = service.create(CreateWordRequest(arabicText = "كَتَبَ", translation = "to write"))

            result.isRight() shouldBe true
            val word = result.getOrNull()!!
            word.arabicText shouldBe "كَتَبَ"
            word.masteryLevel shouldBe MasteryLevel.NEW.name
            coVerify(exactly = 1) { repo.create(any()) }
        }

        test("invalid partOfSpeech string returns UnknownEnumValue") {
            val result = service.create(CreateWordRequest(arabicText = "كَتَبَ", partOfSpeech = "GIBBERISH"))
            result shouldBe DomainError.UnknownEnumValue("partOfSpeech", "GIBBERISH").left()
        }

        test("invalid dialect string returns UnknownEnumValue") {
            val result = service.create(CreateWordRequest(arabicText = "كَتَبَ", dialect = "KLINGON"))
            result shouldBe DomainError.UnknownEnumValue("dialect", "KLINGON").left()
        }

        test("invalid difficulty string returns UnknownEnumValue") {
            val result = service.create(CreateWordRequest(arabicText = "كَتَبَ", difficulty = "HARD"))
            result shouldBe DomainError.UnknownEnumValue("difficulty", "HARD").left()
        }

        test("blank arabicText returns ValidationError") {
            val result = service.create(CreateWordRequest(arabicText = "  "))
            result shouldBe DomainError.ValidationError("arabicText", "Arabic text must not be blank").left()
        }

        test("invalid derivedFromId UUID returns InvalidInput") {
            val result = service.create(CreateWordRequest(arabicText = "كَتَبَ", derivedFromId = "not-a-uuid"))
            result.shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("getById") {
        test("returns response for existing id") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.getById(sampleId.toString())

            result.isRight() shouldBe true
            result.getOrNull()!!.id shouldBe sampleId.toString()
        }

        test("not found propagates") {
            coEvery { repo.findById(WordId(sampleId)) } returns
                DomainError.NotFound("Word", sampleId.toString()).left()

            service.getById(sampleId.toString()) shouldBe
                DomainError.NotFound("Word", sampleId.toString()).left()
        }

        test("malformed UUID returns InvalidInput") {
            service.getById("not-a-uuid").shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("update") {
        test("happy path — updates fields and persists") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { repo.update(any()) } answers { firstArg<Word>().right() }

            val result = service.update(sampleId.toString(), UpdateWordRequest(translation = "to write (updated)"))

            result.isRight() shouldBe true
            result.getOrNull()!!.translation shouldBe "to write (updated)"
        }

        test("updates Arabic text") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { repo.update(any()) } answers { firstArg<Word>().right() }

            val result = service.update(sampleId.toString(), UpdateWordRequest(arabicText = "كَتَبَ"))

            result.isRight() shouldBe true
            result.getOrNull()!!.arabicText shouldBe "كَتَبَ"
        }

        test("blank Arabic text returns ValidationError") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.update(sampleId.toString(), UpdateWordRequest(arabicText = "   "))

            result shouldBe DomainError.ValidationError("arabicText", "Arabic text must not be blank").left()
        }

        test("not found propagates") {
            coEvery { repo.findById(WordId(sampleId)) } returns
                DomainError.NotFound("Word", sampleId.toString()).left()

            service.update(sampleId.toString(), UpdateWordRequest()).shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }

        test("invalid masteryLevel in update returns UnknownEnumValue") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.update(sampleId.toString(), UpdateWordRequest(masteryLevel = "UNKNOWN_LEVEL"))
            result shouldBe DomainError.UnknownEnumValue("masteryLevel", "UNKNOWN_LEVEL").left()
        }
    }

    context("delete") {
        test("happy path") {
            coEvery { repo.delete(WordId(sampleId)) } returns Unit.right()

            service.delete(sampleId.toString()) shouldBe Unit.right()
        }

        test("not found propagates") {
            coEvery { repo.delete(WordId(sampleId)) } returns
                DomainError.NotFound("Word", sampleId.toString()).left()

            service.delete(sampleId.toString()).shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("list") {
        test("delegates to repo with defaults") {
            coEvery { repo.list(PageRequest(1, 20), WordFilters()) } returns
                PaginatedResponse(listOf(sampleWord), 1L, 1, 20).right()

            val result = service.list(null, null, null, null, null, null, null, null, null, null)

            result.isRight() shouldBe true
            result.getOrNull()!!.total shouldBe 1L
        }

        test("passes dialect filter to repo") {
            coEvery { repo.list(PageRequest(1, 20), WordFilters(dialect = Dialect.MSA)) } returns
                PaginatedResponse(listOf(sampleWord), 1L, 1, 20).right()

            service.list(null, null, null, null, "MSA", null, null, null, null, null).isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), WordFilters(dialect = Dialect.MSA)) }
        }
    }

    context("autocomplete") {
        test("delegates to repo and maps to lightweight response") {
            coEvery { repo.autocomplete("كتب", 10, null) } returns listOf(sampleWord).right()

            val result = service.autocomplete("كتب", null)

            result.isRight() shouldBe true
            result.getOrNull()!!.first().arabicText shouldBe "كَتَبَ"
        }
    }

    val sampleNoun = sampleWord.copy(
        id = WordId(UUID.randomUUID()),
        arabicText = "كِتَاب",
        partOfSpeech = PartOfSpeech.NOUN,
    )
    val sampleVerbDetails = VerbDetails(
        wordId = WordId(sampleId),
        verbForm = VerbPattern.I,
        pastPattern = "fa3ala",
        presentPattern = "yaf3ulu",
        weaknessType = WeaknessType.SOUND,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    context("verb details") {
        test("getVerbDetails returns details for a verb") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { verbDetailsRepo.find(WordId(sampleId)) } returns sampleVerbDetails.right()

            val result = service.getVerbDetails(sampleId.toString())

            result.isRight() shouldBe true
            result.getOrNull()!!.verbForm shouldBe "I"
            result.getOrNull()!!.pastPattern shouldBe "fa3ala"
        }

        test("getVerbDetails rejects non-verb") {
            coEvery { repo.findById(sampleNoun.id) } returns sampleNoun.right()

            val result = service.getVerbDetails(sampleNoun.id.toString())

            result.isLeft() shouldBe true
            val error = result.leftOrNull()!!
            error.shouldBeInstanceOf<DomainError.ValidationError>()
            (error as DomainError.ValidationError).field shouldBe "partOfSpeech"
        }

        test("upsertVerbDetails saves for a verb") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { verbDetailsRepo.find(WordId(sampleId)) } returns null.right()
            coEvery { verbDetailsRepo.upsert(any()) } answers { firstArg<VerbDetails>().right() }

            val result = service.upsertVerbDetails(
                sampleId.toString(),
                UpsertVerbDetailsRequest(verbForm = "I", pastPattern = "fa3ala", presentPattern = "yaf3ulu"),
            )

            result.isRight() shouldBe true
            result.getOrNull()!!.verbForm shouldBe "I"
            coVerify(exactly = 1) { verbDetailsRepo.upsert(any()) }
        }

        test("upsertVerbDetails rejects non-verb") {
            coEvery { repo.findById(sampleNoun.id) } returns sampleNoun.right()

            val result = service.upsertVerbDetails(
                sampleNoun.id.toString(),
                UpsertVerbDetailsRequest(verbForm = "I"),
            )

            result.isLeft() shouldBe true
            result.leftOrNull()!!.shouldBeInstanceOf<DomainError.ValidationError>()
            coVerify(exactly = 0) { verbDetailsRepo.upsert(any()) }
        }

        test("deleteVerbDetails rejects non-verb") {
            coEvery { repo.findById(sampleNoun.id) } returns sampleNoun.right()

            val result = service.deleteVerbDetails(sampleNoun.id.toString())

            result.isLeft() shouldBe true
            result.leftOrNull()!!.shouldBeInstanceOf<DomainError.ValidationError>()
            coVerify(exactly = 0) { verbDetailsRepo.delete(any()) }
        }

        test("upsertVerbDetails with invalid verbForm returns ValidationError") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.upsertVerbDetails(
                sampleId.toString(),
                UpsertVerbDetailsRequest(verbForm = "XI"),
            )

            result shouldBe DomainError.UnknownEnumValue("verbForm", "XI").left()
        }

        test("upsertVerbDetails with invalid weaknessType returns UnknownEnumValue") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.upsertVerbDetails(
                sampleId.toString(),
                UpsertVerbDetailsRequest(verbForm = "I", weaknessType = "BROKEN"),
            )

            result shouldBe DomainError.UnknownEnumValue("weaknessType", "BROKEN").left()
        }
    }

    context("dictionary links") {
        test("getDictionaryLinks returns links for word") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { repo.findDictionaryLinks(WordId(sampleId)) } returns listOf(sampleLink).right()

            val result = service.getDictionaryLinks(sampleId.toString())

            result.isRight() shouldBe true
            result.getOrNull()!!.size shouldBe 1
        }

        test("addDictionaryLink happy path") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { repo.addDictionaryLink(any()) } returns sampleLink.right()

            val result = service.addDictionaryLink(
                sampleId.toString(),
                CreateDictionaryLinkRequest("ALMANY", "https://www.almany.de/")
            )

            result.isRight() shouldBe true
        }

        test("addDictionaryLink with invalid source returns UnknownEnumValue") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()

            val result = service.addDictionaryLink(
                sampleId.toString(),
                CreateDictionaryLinkRequest("BADLINK", "https://example.com")
            )
            result shouldBe DomainError.UnknownEnumValue("source", "BADLINK").left()
        }

        test("deleteDictionaryLink happy path") {
            coEvery { repo.findById(WordId(sampleId)) } returns sampleWord.right()
            coEvery { repo.deleteDictionaryLink(WordId(sampleId), DictionaryLinkId(sampleLinkId)) } returns Unit.right()

            service.deleteDictionaryLink(sampleId.toString(), sampleLinkId.toString()) shouldBe Unit.right()
        }
    }
})
