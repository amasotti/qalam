package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.domain.word.DictionaryLink
import com.tonihacks.qalam.domain.word.DictionaryLinkId
import com.tonihacks.qalam.domain.word.DictionarySource
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordExample
import com.tonihacks.qalam.domain.word.WordExampleId
import com.tonihacks.qalam.domain.word.WordFilters
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedWordRepository : WordRepository {

    override suspend fun findById(id: WordId): Either<DomainError, Word> =
        suspendTransaction {
            WordsTable
                .selectAll()
                .where { WordsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?.toWord()
                ?.right()
                ?: DomainError.NotFound("Word", id.toString()).left()
        }

    override suspend fun list(page: PageRequest, filters: WordFilters): Either<DomainError, PaginatedResponse<Word>> =
        suspendTransaction {
            val query = WordsTable.selectAll().let { q ->
                var condition: Op<Boolean>? = null

                filters.dialect?.let { d ->
                    condition = condition?.and(WordsTable.dialect eq d.name) ?: (WordsTable.dialect eq d.name)
                }
                filters.difficulty?.let { d ->
                    condition = condition?.and(WordsTable.difficulty eq d.name) ?: (WordsTable.difficulty eq d.name)
                }
                filters.partOfSpeech?.let { p ->
                    condition = condition?.and(WordsTable.partOfSpeech eq p.name) ?: (WordsTable.partOfSpeech eq p.name)
                }
                filters.masteryLevel?.let { m ->
                    condition = condition?.and(WordsTable.masteryLevel eq m.name) ?: (WordsTable.masteryLevel eq m.name)
                }
                filters.q?.let { queryStr ->
                    val qCondition = (WordsTable.arabicText ilike "%$queryStr%") or
                        (WordsTable.translation ilike "%$queryStr%") or
                        (WordsTable.transliteration ilike "%$queryStr%")
                    condition = condition?.and(qCondition) ?: qCondition
                }

                condition?.let { q.where { it } } ?: q
            }

            val total = query.count()
            val items = query
                .limit(page.size)
                .offset(page.offset)
                .map { it.toWord() }

            PaginatedResponse(items = items, total = total, page = page.page, size = page.size).right()
        }

    override suspend fun autocomplete(query: String, limit: Int): Either<DomainError, List<Word>> =
        suspendTransaction {
            WordsTable
                .selectAll()
                .where {
                    (WordsTable.arabicText ilike "%$query%") or
                    (WordsTable.translation ilike "%$query%") or
                    (WordsTable.transliteration ilike "%$query%")
                }
                .limit(limit)
                .map { it.toWord() }
                .right()
        }

    override suspend fun create(word: Word): Either<DomainError, Word> =
        suspendTransaction {
            try {
                WordsTable.insert {
                    it[id] = word.id.value.toKotlinUuid()
                    it[arabicText] = word.arabicText
                    it[transliteration] = word.transliteration
                    it[translation] = word.translation
                    it[partOfSpeech] = word.partOfSpeech.name
                    it[dialect] = word.dialect.name
                    it[difficulty] = word.difficulty.name
                    it[masteryLevel] = word.masteryLevel.name
                    it[pronunciationUrl] = word.pronunciationUrl
                    it[rootId] = word.rootId?.value?.toKotlinUuid()
                    it[derivedFromId] = word.derivedFromId?.value?.toKotlinUuid()
                }
                // Create word_progress atomically in the same transaction
                WordProgressTable.insert {
                    it[wordId] = word.id.value.toKotlinUuid()
                    it[consecutiveCorrect] = 0
                    it[totalAttempts] = 0
                    it[totalCorrect] = 0
                    it[lastReviewedAt] = null
                }
                word.right()
            } catch (e: java.sql.SQLException) {
                // SQLState 23xxx = integrity constraint violation (FK, unique, not-null, check)
                if (e.sqlState?.startsWith("23") == true) {
                    DomainError.NotFound("ArabicRoot", word.rootId?.toString() ?: "unknown").left()
                } else {
                    DomainError.DatabaseError.left()
                }
            }
        }

    override suspend fun update(word: Word): Either<DomainError, Word> =
        suspendTransaction {
            either {
                val wordId = word.id.value
                val updatedCount = WordsTable.update({ WordsTable.id eq wordId.toKotlinUuid() }) {
                    it[arabicText] = word.arabicText
                    it[transliteration] = word.transliteration
                    it[translation] = word.translation
                    it[partOfSpeech] = word.partOfSpeech.name
                    it[dialect] = word.dialect.name
                    it[difficulty] = word.difficulty.name
                    it[masteryLevel] = word.masteryLevel.name
                    it[pronunciationUrl] = word.pronunciationUrl
                    it[rootId] = word.rootId?.value?.toKotlinUuid()
                    it[derivedFromId] = word.derivedFromId?.value?.toKotlinUuid()
                }

                ensure(updatedCount > 0) { DomainError.NotFound("Word", wordId.toString()) }

                WordsTable.selectAll()
                    .where { WordsTable.id eq wordId.toKotlinUuid() }
                    .singleOrNull()
                    ?.toWord()
                    .let { refreshed ->
                        ensureNotNull(refreshed) { DomainError.NotFound("Word", wordId.toString()) }
                    }
            }
        }

    override suspend fun delete(id: WordId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = WordsTable.deleteWhere { WordsTable.id eq id.value.toKotlinUuid() }
                ensure(deleteCount > 0) { DomainError.NotFound("Word", id.toString()) }
            }
        }

    override suspend fun findDictionaryLinks(wordId: WordId): Either<DomainError, List<DictionaryLink>> =
        suspendTransaction {
            WordDictionaryLinksTable
                .selectAll()
                .where { WordDictionaryLinksTable.wordId eq wordId.value.toKotlinUuid() }
                .map { it.toDictionaryLink() }
                .right()
        }

    override suspend fun addDictionaryLink(link: DictionaryLink): Either<DomainError, DictionaryLink> =
        suspendTransaction {
            WordDictionaryLinksTable.insert {
                it[id] = link.id.value.toKotlinUuid()
                it[wordId] = link.wordId.value.toKotlinUuid()
                it[linkSource] = link.source.name
                it[url] = link.url
            }
            link.right()
        }

    override suspend fun deleteDictionaryLink(wordId: WordId, linkId: DictionaryLinkId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = WordDictionaryLinksTable.deleteWhere {
                    (WordDictionaryLinksTable.id eq linkId.value.toKotlinUuid()) and
                    (WordDictionaryLinksTable.wordId eq wordId.value.toKotlinUuid())
                }
                ensure(deleteCount > 0) { DomainError.NotFound("DictionaryLink", linkId.toString()) }
            }
        }

    override suspend fun findExamples(wordId: WordId): Either<DomainError, List<WordExample>> =
        suspendTransaction {
            WordExamplesTable
                .selectAll()
                .where { WordExamplesTable.wordId eq wordId.value.toKotlinUuid() }
                .map { it.toWordExample() }
                .right()
        }

    override suspend fun addExample(example: WordExample): Either<DomainError, WordExample> =
        suspendTransaction {
            WordExamplesTable.insert {
                it[id] = example.id.value.toKotlinUuid()
                it[wordId] = example.wordId.value.toKotlinUuid()
                it[arabic] = example.arabic
                it[transliteration] = example.transliteration
                it[translation] = example.translation
            }
            example.right()
        }

    override suspend fun deleteExample(wordId: WordId, exampleId: WordExampleId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = WordExamplesTable.deleteWhere {
                    (WordExamplesTable.id eq exampleId.value.toKotlinUuid()) and
                    (WordExamplesTable.wordId eq wordId.value.toKotlinUuid())
                }
                ensure(deleteCount > 0) { DomainError.NotFound("WordExample", exampleId.toString()) }
            }
        }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun ResultRow.toWord() = Word(
    id = WordId(this[WordsTable.id].toJavaUuid()),
    arabicText = this[WordsTable.arabicText],
    transliteration = this[WordsTable.transliteration],
    translation = this[WordsTable.translation],
    partOfSpeech = PartOfSpeech.fromString(this[WordsTable.partOfSpeech]) ?: PartOfSpeech.UNKNOWN,
    dialect = Dialect.fromString(this[WordsTable.dialect]) ?: Dialect.MSA,
    difficulty = Difficulty.fromString(this[WordsTable.difficulty]) ?: Difficulty.BEGINNER,
    masteryLevel = MasteryLevel.fromString(this[WordsTable.masteryLevel]) ?: MasteryLevel.NEW,
    pronunciationUrl = this[WordsTable.pronunciationUrl],
    rootId = this[WordsTable.rootId]?.toJavaUuid()?.let { RootId(it) },
    derivedFromId = this[WordsTable.derivedFromId]?.toJavaUuid()?.let { WordId(it) },
    createdAt = this[WordsTable.createdAt],
    updatedAt = this[WordsTable.updatedAt],
)

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun ResultRow.toWordExample() = WordExample(
    id = WordExampleId(this[WordExamplesTable.id].toJavaUuid()),
    wordId = WordId(this[WordExamplesTable.wordId].toJavaUuid()),
    arabic = this[WordExamplesTable.arabic],
    transliteration = this[WordExamplesTable.transliteration],
    translation = this[WordExamplesTable.translation],
    createdAt = this[WordExamplesTable.createdAt],
)

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun ResultRow.toDictionaryLink() = DictionaryLink(
    id = DictionaryLinkId(this[WordDictionaryLinksTable.id].toJavaUuid()),
    wordId = WordId(this[WordDictionaryLinksTable.wordId].toJavaUuid()),
    source = DictionarySource.fromString(this[WordDictionaryLinksTable.linkSource]) ?: DictionarySource.CUSTOM,
    url = this[WordDictionaryLinksTable.url],
)
