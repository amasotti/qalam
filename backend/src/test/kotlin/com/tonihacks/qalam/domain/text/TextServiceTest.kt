package com.tonihacks.qalam.domain.text

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Instant

// ─── test-data factory ───────────────────────────────────────────────────────

private fun aText(
    id: TextId = TextId(UUID.randomUUID()),
    title: String = "أهل الكهف",
    body: String = "وَإِذِ اعْتَزَلْتُمُوهُمْ",
    transliteration: String? = "wa-idh i'tazaltumuhum",
    translation: String? = "And when you have withdrawn from them",
    difficulty: Difficulty = Difficulty.INTERMEDIATE,
    dialect: Dialect = Dialect.MSA,
    comments: String? = "Quranic passage",
    tags: List<String> = listOf("quran", "classical"),
    createdAt: Instant = Instant.fromEpochMilliseconds(0),
    updatedAt: Instant = Instant.fromEpochMilliseconds(0),
) = Text(
    id = id,
    title = title,
    body = body,
    transliteration = transliteration,
    translation = translation,
    difficulty = difficulty,
    dialect = dialect,
    comments = comments,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ─── tests ───────────────────────────────────────────────────────────────────

class TextServiceTest : FreeSpec({

    val repo = mockk<TextRepository>()
    val service = TextService(repo)

    beforeTest { clearMocks(repo) }

    val sampleId = UUID.randomUUID()
    val sampleText = aText(id = TextId(sampleId))

    "getById" - {

        "returns text when found" {
            coEvery { repo.findById(TextId(sampleId)) } returns sampleText.right()

            val result = service.getById(sampleId.toString())

            result.isRight() shouldBe true
            result.getOrNull()!!.id shouldBe sampleText.id
        }

        "returns NotFound when repo returns error" {
            val err = DomainError.NotFound("Text", sampleId.toString())
            coEvery { repo.findById(TextId(sampleId)) } returns err.left()

            service.getById(sampleId.toString()) shouldBe err.left()
        }

        "returns InvalidInput for malformed UUID" {
            val result = service.getById("not-a-uuid")
            result.shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    "create" - {

        "calls repo.save with correct fields and returns created text" {
            coEvery { repo.save(any()) } answers { firstArg<Text>().right() }

            val result = service.create(
                title = "  نص جديد  ",
                body = "محتوى النص",
                transliteration = "nass jadid",
                translation = "A new text",
                difficulty = "BEGINNER",
                dialect = "MSA",
                comments = null,
                tags = listOf("new", "test"),
            )

            result.isRight() shouldBe true
            val text = result.getOrNull()!!
            text.title shouldBe "نص جديد"          // trimmed
            text.difficulty shouldBe Difficulty.BEGINNER
            text.dialect shouldBe Dialect.MSA
            coVerify(exactly = 1) { repo.save(any()) }
        }

        "tags are passed through (deduped and trimmed)" {
            coEvery { repo.save(any()) } answers { firstArg<Text>().right() }

            val result = service.create(
                title = "عنوان",
                body = "محتوى",
                transliteration = null,
                translation = null,
                difficulty = "BEGINNER",
                dialect = "MSA",
                comments = null,
                tags = listOf("  tag1  ", "tag1", "tag2", ""),
            )

            result.isRight() shouldBe true
            result.getOrNull()!!.tags shouldBe listOf("tag1", "tag2")
        }

        "returns ValidationError for blank title" {
            val result = service.create(
                title = "   ",
                body = "محتوى",
                transliteration = null,
                translation = null,
                difficulty = "BEGINNER",
                dialect = "MSA",
                comments = null,
                tags = emptyList(),
            )
            result shouldBe DomainError.ValidationError("title", "Title must not be blank").left()
        }

        "returns ValidationError for blank body" {
            val result = service.create(
                title = "عنوان",
                body = "  ",
                transliteration = null,
                translation = null,
                difficulty = "BEGINNER",
                dialect = "MSA",
                comments = null,
                tags = emptyList(),
            )
            result shouldBe DomainError.ValidationError("body", "Body must not be blank").left()
        }

        "returns ValidationError for unknown difficulty" {
            val result = service.create(
                title = "عنوان",
                body = "محتوى",
                transliteration = null,
                translation = null,
                difficulty = "GIBBERISH",
                dialect = "MSA",
                comments = null,
                tags = emptyList(),
            )
            result shouldBe DomainError.ValidationError("difficulty", "Unknown value: GIBBERISH").left()
        }

        "returns ValidationError for unknown dialect" {
            val result = service.create(
                title = "عنوان",
                body = "محتوى",
                transliteration = null,
                translation = null,
                difficulty = "BEGINNER",
                dialect = "KLINGON",
                comments = null,
                tags = emptyList(),
            )
            result shouldBe DomainError.ValidationError("dialect", "Unknown value: KLINGON").left()
        }
    }

    "update" - {

        "applies non-null fields and calls repo.update" {
            coEvery { repo.findById(TextId(sampleId)) } returns sampleText.right()
            coEvery { repo.update(any()) } answers { firstArg<Text>().right() }

            val result = service.update(
                id = sampleId.toString(),
                title = "عنوان محدث",
                body = null,
                transliteration = null,
                translation = null,
                difficulty = null,
                dialect = null,
                comments = null,
                tags = null,
            )

            result.isRight() shouldBe true
            val text = result.getOrNull()!!
            text.title shouldBe "عنوان محدث"
            text.body shouldBe sampleText.body   // unchanged
            coVerify(exactly = 1) { repo.update(any()) }
        }

        "tags replaced when tags param is provided" {
            coEvery { repo.findById(TextId(sampleId)) } returns sampleText.right()
            coEvery { repo.update(any()) } answers { firstArg<Text>().right() }

            val result = service.update(
                id = sampleId.toString(),
                title = null,
                body = null,
                transliteration = null,
                translation = null,
                difficulty = null,
                dialect = null,
                comments = null,
                tags = listOf("replaced"),
            )

            result.isRight() shouldBe true
            result.getOrNull()!!.tags shouldBe listOf("replaced")
        }

        "tags unchanged when tags param is null" {
            coEvery { repo.findById(TextId(sampleId)) } returns sampleText.right()
            coEvery { repo.update(any()) } answers { firstArg<Text>().right() }

            val result = service.update(
                id = sampleId.toString(),
                title = null,
                body = null,
                transliteration = null,
                translation = null,
                difficulty = null,
                dialect = null,
                comments = null,
                tags = null,
            )

            result.isRight() shouldBe true
            result.getOrNull()!!.tags shouldBe sampleText.tags
        }

        "returns NotFound when text doesn't exist" {
            val err = DomainError.NotFound("Text", sampleId.toString())
            coEvery { repo.findById(TextId(sampleId)) } returns err.left()

            val result = service.update(
                id = sampleId.toString(),
                title = "anything",
                body = null,
                transliteration = null,
                translation = null,
                difficulty = null,
                dialect = null,
                comments = null,
                tags = null,
            )

            result shouldBe err.left()
        }

        "returns ValidationError for unknown difficulty in update" {
            coEvery { repo.findById(TextId(sampleId)) } returns sampleText.right()

            val result = service.update(
                id = sampleId.toString(),
                title = null,
                body = null,
                transliteration = null,
                translation = null,
                difficulty = "ULTRA",
                dialect = null,
                comments = null,
                tags = null,
            )

            result shouldBe DomainError.ValidationError("difficulty", "Unknown value: ULTRA").left()
        }
    }

    "delete" - {

        "calls repo.delete and returns Unit" {
            coEvery { repo.delete(TextId(sampleId)) } returns Unit.right()

            service.delete(sampleId.toString()) shouldBe Unit.right()
            coVerify(exactly = 1) { repo.delete(TextId(sampleId)) }
        }

        "returns NotFound when text doesn't exist" {
            val err = DomainError.NotFound("Text", sampleId.toString())
            coEvery { repo.delete(TextId(sampleId)) } returns err.left()

            service.delete(sampleId.toString()) shouldBe err.left()
        }

        "returns InvalidInput for malformed UUID" {
            val result = service.delete("not-a-uuid")
            result.shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    "list" - {

        "passes filters through to repo and returns paginated result" {
            coEvery { repo.list(PageRequest(1, 20), TextFilters()) } returns
                PaginatedResponse(listOf(sampleText), 1L, 1, 20).right()

            val result = service.list(null, null, null, null, null, null)

            result.isRight() shouldBe true
            result.getOrNull()!!.total shouldBe 1L
            coVerify { repo.list(PageRequest(1, 20), TextFilters()) }
        }

        "passes dialect filter to repo" {
            coEvery { repo.list(PageRequest(1, 20), TextFilters(dialect = Dialect.MSA)) } returns
                PaginatedResponse(listOf(sampleText), 1L, 1, 20).right()

            service.list(null, null, null, "MSA", null, null).isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), TextFilters(dialect = Dialect.MSA)) }
        }

        "passes difficulty filter to repo" {
            coEvery {
                repo.list(PageRequest(1, 20), TextFilters(difficulty = Difficulty.INTERMEDIATE))
            } returns PaginatedResponse(listOf(sampleText), 1L, 1, 20).right()

            service.list(null, null, null, null, "INTERMEDIATE", null).isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), TextFilters(difficulty = Difficulty.INTERMEDIATE)) }
        }

        "passes tag filter to repo" {
            coEvery { repo.list(PageRequest(1, 20), TextFilters(tag = "quran")) } returns
                PaginatedResponse(listOf(sampleText), 1L, 1, 20).right()

            service.list(null, null, null, null, null, "quran").isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), TextFilters(tag = "quran")) }
        }

        "passes q (free-text) filter to repo" {
            coEvery { repo.list(PageRequest(1, 20), TextFilters(q = "كهف")) } returns
                PaginatedResponse(listOf(sampleText), 1L, 1, 20).right()

            service.list(null, null, "كهف", null, null, null).isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), TextFilters(q = "كهف")) }
        }

        "returns ValidationError for unknown dialect filter" {
            val result = service.list(null, null, null, "KLINGON", null, null)
            result shouldBe DomainError.ValidationError("dialect", "Unknown value: KLINGON").left()
        }
    }
})
