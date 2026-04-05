package com.tonihacks.qalam.domain.root

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.root.CreateRootRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeRequest
import com.tonihacks.qalam.delivery.dto.root.UpdateRootRequest
import com.tonihacks.qalam.domain.error.DomainError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Instant

class RootServiceTest : FunSpec({

    val repo = mockk<RootRepository>()
    val service = RootService(repo)

    beforeTest { clearMocks(repo) }

    val sampleId = UUID.randomUUID()
    val r7ab = "رحب"
    val r7abLetters = listOf("ر", "ح", "ب")
    val r7abDisplayForm = "ر-ح-ب"
    val sampleRoot = ArabicRoot(
        id = RootId(sampleId),
        letters = r7abLetters,
        normalizedForm = r7ab,
        displayForm = r7abDisplayForm,
        letterCount = 3,
        meaning = "openness",
        analysis = null,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

    context("create") {
        test("happy path — normalizes, checks uniqueness, persists") {
            coEvery { repo.existsByNormalizedForm(r7ab) } returns false.right()
            coEvery { repo.create(any()) } answers { firstArg<ArabicRoot>().right() }

            val result = service.create(CreateRootRequest(r7abLetters, "openness", null))

            result.isRight() shouldBe true
            coVerify(exactly = 1) { repo.create(any()) }
        }

        test("duplicate normalized form returns AlreadyExists") {
            coEvery { repo.existsByNormalizedForm(r7ab) } returns true.right()

            val result = service.create(CreateRootRequest(r7abLetters, null, null))

            result shouldBe DomainError.AlreadyExists("ArabicRoot", "root 'ر-ح-ب' already exists").left()
            coVerify(exactly = 0) { repo.create(any()) }
        }

        test("invalid letters propagate as InvalidInput") {
            val result = service.create(CreateRootRequest(listOf("r", "ح", "ب"), null, null))
            result.shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("getById") {
        test("returns response for existing id") {
            coEvery { repo.findById(RootId(sampleId)) } returns sampleRoot.right()

            val result = service.getById(sampleId.toString())

            result.isRight() shouldBe true
            result.getOrNull()!!.id shouldBe sampleId.toString()
        }

        test("not found propagates") {
            coEvery { repo.findById(RootId(sampleId)) } returns DomainError.NotFound("ArabicRoot", sampleId.toString()).left()

            service.getById(sampleId.toString()) shouldBe
                DomainError.NotFound("ArabicRoot", sampleId.toString()).left()
        }

        test("malformed UUID returns InvalidInput") {
            service.getById("not-a-uuid").shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("update") {
        test("happy path — fetches, updates, persists") {
            coEvery { repo.findById(RootId(sampleId)) } returns sampleRoot.right()
            coEvery { repo.update(any()) } answers { firstArg<ArabicRoot>().right() }

            val result = service.update(sampleId.toString(), UpdateRootRequest("new meaning", null))

            result.isRight() shouldBe true
            result.getOrNull()!!.meaning shouldBe "new meaning"
        }

        test("not found propagates") {
            coEvery { repo.findById(RootId(sampleId)) } returns DomainError.NotFound("ArabicRoot", sampleId.toString()).left()

            service.update(sampleId.toString(), UpdateRootRequest(null, null))
                .shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("delete") {
        test("happy path") {
            coEvery { repo.delete(RootId(sampleId)) } returns Unit.right()

            service.delete(sampleId.toString()) shouldBe Unit.right()
        }

        test("not found propagates") {
            coEvery { repo.delete(RootId(sampleId)) } returns DomainError.NotFound("ArabicRoot", sampleId.toString()).left()

            service.delete(sampleId.toString()).shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("normalize") {
        test("space-separated returns correct forms") {
            val result = service.normalize(NormalizeRequest("ر ح ب"))
            result.isRight() shouldBe true
            result.getOrNull()!!.normalizedForm shouldBe r7ab
            result.getOrNull()!!.displayForm shouldBe r7abDisplayForm
        }

        test("dash-separated returns same output") {
            service.normalize(NormalizeRequest(r7abDisplayForm)) shouldBe
                service.normalize(NormalizeRequest("ر ح ب"))
        }

        test("invalid input returns error") {
            service.normalize(NormalizeRequest("abc")).shouldBeInstanceOf<arrow.core.Either.Left<*>>()
        }
    }

    context("list") {
        test("delegates to repo with parsed pagination") {
            coEvery { repo.list(PageRequest(1, 20), null) } returns
                PaginatedResponse(listOf(sampleRoot), 1L, 1, 20).right()

            val result = service.list(null, null, null)

            result.isRight() shouldBe true
            result.getOrNull()!!.total shouldBe 1L
        }

        test("passes letterCount filter to repo") {
            coEvery { repo.list(PageRequest(1, 20), 3) } returns
                PaginatedResponse(listOf(sampleRoot), 1L, 1, 20).right()

            service.list(null, null, 3).isRight() shouldBe true
            coVerify { repo.list(PageRequest(1, 20), 3) }
        }
    }
})
