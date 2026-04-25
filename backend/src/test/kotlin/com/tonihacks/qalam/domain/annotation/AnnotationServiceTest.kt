package com.tonihacks.qalam.domain.annotation

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
private fun anAnnotation(
    textId: TextId = TextId(UUID.randomUUID()),
    anchor: String = "كلمة",
    type: AnnotationType = AnnotationType.VOCABULARY,
    content: String? = null,
    linkedWordIds: List<WordId> = emptyList(),
) = Annotation(
    id = AnnotationId(UUID.randomUUID()),
    textId = textId,
    anchor = anchor,
    type = type,
    content = content,
    linkedWordIds = linkedWordIds,
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),
)

// ─── tests ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
class AnnotationServiceTest : FreeSpec({

    val repo = mockk<AnnotationRepository>()
    val service = AnnotationService(repo)

    beforeTest { clearMocks(repo) }

    val sample = anAnnotation()
    val sampleId = sample.id
    val sampleTextId = sample.textId

    "listByText" - {

        "delegates to repo and returns list on success" {
            coEvery { repo.findAllByTextId(sampleTextId) } returns listOf(sample).right()

            val result = service.listByText(sampleTextId)

            result.isRight() shouldBe true
            result.getOrNull()!! shouldBe listOf(sample)
            coVerify(exactly = 1) { repo.findAllByTextId(sampleTextId) }
        }

        "propagates error from repo" {
            val err = DomainError.DatabaseError
            coEvery { repo.findAllByTextId(sampleTextId) } returns err.left()

            service.listByText(sampleTextId) shouldBe err.left()
        }
    }

    "getById" - {

        "returns annotation on success" {
            coEvery { repo.findById(sampleId) } returns sample.right()

            val result = service.getById(sampleId)

            result.isRight() shouldBe true
            result.getOrNull()!! shouldBe sample
        }

        "returns NotFound when repo returns error" {
            val err = DomainError.NotFound("Annotation", sampleId.toString())
            coEvery { repo.findById(sampleId) } returns err.left()

            service.getById(sampleId) shouldBe err.left()
        }
    }

    "create" - {

        "blank anchor returns ValidationError without calling repo" {
            val result = service.create(
                textId = sampleTextId,
                anchor = "   ",
                type = AnnotationType.VOCABULARY,
                content = null,
                linkedWordIds = emptyList(),
            )

            result shouldBe DomainError.ValidationError("anchor", "anchor must not be blank").left()
            coVerify(exactly = 0) { repo.save(any()) }
        }

        "valid input calls repo.save with correct fields" {
            coEvery { repo.save(any()) } answers { firstArg<Annotation>().right() }

            val result = service.create(
                textId = sampleTextId,
                anchor = "بِسْمِ",
                type = AnnotationType.GRAMMAR,
                content = "preposition + noun",
                linkedWordIds = emptyList(),
            )

            result.isRight() shouldBe true
            val saved = result.getOrNull()!!
            saved.textId shouldBe sampleTextId
            saved.anchor shouldBe "بِسْمِ"
            saved.type shouldBe AnnotationType.GRAMMAR
            saved.content shouldBe "preposition + noun"
            coVerify(exactly = 1) { repo.save(any()) }
        }
    }

    "update" - {

        "returns NotFound when annotation doesn't exist" {
            val err = DomainError.NotFound("Annotation", sampleId.toString())
            coEvery { repo.findById(sampleId) } returns err.left()

            service.update(id = sampleId, anchor = "جديد") shouldBe err.left()
        }

        "blank anchor on update returns ValidationError without calling repo.update" {
            coEvery { repo.findById(sampleId) } returns sample.right()

            val result = service.update(id = sampleId, anchor = "  ")

            result shouldBe DomainError.ValidationError("anchor", "anchor must not be blank").left()
            coVerify(exactly = 0) { repo.update(any()) }
        }

        "partial update: only type provided — other fields unchanged" {
            val existing = anAnnotation(anchor = "النص", type = AnnotationType.VOCABULARY, content = "some content")
            coEvery { repo.findById(existing.id) } returns existing.right()
            coEvery { repo.update(any()) } answers { firstArg<Annotation>().right() }

            val result = service.update(id = existing.id, type = AnnotationType.GRAMMAR)

            result.isRight() shouldBe true
            val updated = result.getOrNull()!!
            updated.type shouldBe AnnotationType.GRAMMAR
            updated.anchor shouldBe "النص"
            updated.content shouldBe "some content"
        }
    }

    "delete" - {

        "calls repo.delete and returns Unit on success" {
            coEvery { repo.delete(sampleId) } returns Unit.right()

            service.delete(sampleId) shouldBe Unit.right()
            coVerify(exactly = 1) { repo.delete(sampleId) }
        }

        "propagates NotFound from repo" {
            val err = DomainError.NotFound("Annotation", sampleId.toString())
            coEvery { repo.delete(sampleId) } returns err.left()

            service.delete(sampleId) shouldBe err.left()
        }
    }

    "addWordLink" - {

        "delegates to repo.addWordLink" {
            val wordId = WordId(UUID.randomUUID())
            val updated = anAnnotation(linkedWordIds = listOf(wordId))
            coEvery { repo.addWordLink(sampleId, wordId) } returns updated.right()

            val result = service.addWordLink(sampleId, wordId)

            result.isRight() shouldBe true
            result.getOrNull()!!.linkedWordIds shouldBe listOf(wordId)
            coVerify(exactly = 1) { repo.addWordLink(sampleId, wordId) }
        }

        "propagates NotFound when annotation doesn't exist" {
            val wordId = WordId(UUID.randomUUID())
            val err = DomainError.NotFound("Annotation", sampleId.toString())
            coEvery { repo.addWordLink(sampleId, wordId) } returns err.left()

            service.addWordLink(sampleId, wordId) shouldBe err.left()
        }
    }

    "removeWordLink" - {

        "delegates to repo.removeWordLink" {
            val wordId = WordId(UUID.randomUUID())
            val updated = anAnnotation(linkedWordIds = emptyList())
            coEvery { repo.removeWordLink(sampleId, wordId) } returns updated.right()

            val result = service.removeWordLink(sampleId, wordId)

            result.isRight() shouldBe true
            result.getOrNull()!!.linkedWordIds shouldBe emptyList()
            coVerify(exactly = 1) { repo.removeWordLink(sampleId, wordId) }
        }
    }

    "getAnnotationsForWord" - {

        "delegates to repo.findAllByWordId" {
            val wordId = WordId(UUID.randomUUID())
            coEvery { repo.findAllByWordId(wordId) } returns listOf(sample).right()

            val result = service.getAnnotationsForWord(wordId)

            result.isRight() shouldBe true
            result.getOrNull()!! shouldBe listOf(sample)
            coVerify(exactly = 1) { repo.findAllByWordId(wordId) }
        }
    }
})
