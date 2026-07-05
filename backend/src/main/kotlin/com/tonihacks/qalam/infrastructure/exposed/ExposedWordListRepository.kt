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
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.wordlist.WordList
import com.tonihacks.qalam.domain.wordlist.WordListId
import com.tonihacks.qalam.domain.wordlist.WordListRepository
import com.tonihacks.qalam.domain.wordlist.WordListSummary
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedWordListRepository : WordListRepository {

    override suspend fun list(page: PageRequest): Either<DomainError, PaginatedResponse<WordListSummary>> =
        suspendTransaction {
            val total = WordListsTable.selectAll().count()
            val lists = WordListsTable
                .selectAll()
                .orderBy(WordListsTable.createdAt, SortOrder.DESC)
                .limit(page.size)
                .offset(page.offset)
                .map { it.toWordList() }

            val counts = if (lists.isEmpty()) {
                emptyMap()
            } else {
                WordListItemsTable
                    .selectAll()
                    .where { WordListItemsTable.listId inList lists.map { it.id.value.toKotlinUuid() } }
                    .groupBy { it[WordListItemsTable.listId] }
                    .mapValues { it.value.size.toLong() }
            }

            PaginatedResponse(
                items = lists.map { l ->
                    WordListSummary(l, counts[l.id.value.toKotlinUuid()] ?: 0L)
                },
                total = total,
                page = page.page,
                size = page.size,
            ).right()
        }

    override suspend fun findById(id: WordListId): Either<DomainError, WordList> =
        suspendTransaction {
            WordListsTable
                .selectAll()
                .where { WordListsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?.toWordList()
                ?.right()
                ?: DomainError.NotFound("WordList", id.toString()).left()
        }

    override suspend fun create(list: WordList): Either<DomainError, WordList> =
        suspendTransaction {
            WordListsTable.insert {
                it[id] = list.id.value.toKotlinUuid()
                it[title] = list.title
                it[description] = list.description
                it[createdAt] = list.createdAt
                it[updatedAt] = list.updatedAt
            }
            list.right()
        }

    override suspend fun update(list: WordList): Either<DomainError, WordList> =
        suspendTransaction {
            either {
                val updated = WordListsTable.update({ WordListsTable.id eq list.id.value.toKotlinUuid() }) {
                    it[title] = list.title
                    it[description] = list.description
                }
                ensure(updated > 0) { DomainError.NotFound("WordList", list.id.toString()) }

                WordListsTable.selectAll()
                    .where { WordListsTable.id eq list.id.value.toKotlinUuid() }
                    .singleOrNull()
                    ?.toWordList()
                    .let { refreshed ->
                        ensureNotNull(refreshed) { DomainError.NotFound("WordList", list.id.toString()) }
                    }
            }
        }

    override suspend fun delete(id: WordListId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleted = WordListsTable.deleteWhere { WordListsTable.id eq id.value.toKotlinUuid() }
                ensure(deleted > 0) { DomainError.NotFound("WordList", id.toString()) }
                Unit
            }
        }

    override suspend fun membersOf(id: WordListId): Either<DomainError, List<Word>> =
        suspendTransaction {
            WordListItemsTable
                .join(WordsTable, JoinType.INNER, additionalConstraint = { WordListItemsTable.wordId eq WordsTable.id })
                .selectAll()
                .where { WordListItemsTable.listId eq id.value.toKotlinUuid() }
                .orderBy(WordListItemsTable.position, SortOrder.ASC)
                .map { it.toWord() }
                .right()
        }

    override suspend fun addWord(listId: WordListId, wordId: WordId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val kList = listId.value.toKotlinUuid()
                val kWord = wordId.value.toKotlinUuid()

                val listExists = WordListsTable.selectAll()
                    .where { WordListsTable.id eq kList }.empty().not()
                ensure(listExists) { DomainError.NotFound("WordList", listId.toString()) }

                val wordExists = WordsTable.selectAll()
                    .where { WordsTable.id eq kWord }.empty().not()
                ensure(wordExists) { DomainError.NotFound("Word", wordId.toString()) }

                val alreadyMember = WordListItemsTable.selectAll()
                    .where { (WordListItemsTable.listId eq kList) and (WordListItemsTable.wordId eq kWord) }
                    .empty().not()
                ensure(!alreadyMember) {
                    DomainError.AlreadyExists("WordListItem", "word is already in this list")
                }

                val nextPosition = WordListItemsTable.selectAll()
                    .where { WordListItemsTable.listId eq kList }
                    .maxOfOrNull { it[WordListItemsTable.position] }
                    ?.plus(1) ?: 0

                WordListItemsTable.insert {
                    it[WordListItemsTable.listId] = kList
                    it[WordListItemsTable.wordId] = kWord
                    it[position] = nextPosition
                }
                Unit
            }
        }

    override suspend fun removeWord(listId: WordListId, wordId: WordId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleted = WordListItemsTable.deleteWhere {
                    (WordListItemsTable.listId eq listId.value.toKotlinUuid()) and
                        (WordListItemsTable.wordId eq wordId.value.toKotlinUuid())
                }
                ensure(deleted > 0) {
                    DomainError.NotFound("WordListItem", "${listId}/${wordId}")
                }
                Unit
            }
        }

    override suspend fun listsForWord(wordId: WordId): Either<DomainError, List<WordList>> =
        suspendTransaction {
            WordListItemsTable
                .join(WordListsTable, JoinType.INNER, additionalConstraint = { WordListItemsTable.listId eq WordListsTable.id })
                .selectAll()
                .where { WordListItemsTable.wordId eq wordId.value.toKotlinUuid() }
                .orderBy(WordListsTable.title, SortOrder.ASC)
                .map { it.toWordList() }
                .right()
        }
}

private fun ResultRow.toWordList() = WordList(
    id = WordListId(this[WordListsTable.id].toJavaUuid()),
    title = this[WordListsTable.title],
    description = this[WordListsTable.description],
    createdAt = this[WordListsTable.createdAt],
    updatedAt = this[WordListsTable.updatedAt],
)
