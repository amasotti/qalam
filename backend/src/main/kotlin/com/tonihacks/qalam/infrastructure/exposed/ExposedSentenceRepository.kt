package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.sentence.AlignmentToken
import com.tonihacks.qalam.domain.sentence.AlignmentTokenId
import com.tonihacks.qalam.domain.sentence.Sentence
import com.tonihacks.qalam.domain.sentence.SentenceId
import com.tonihacks.qalam.domain.sentence.SentenceRepository
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid as KotlinUUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ExposedSentenceRepository : SentenceRepository {

    override suspend fun findAllByTextId(textId: TextId): Either<DomainError, List<Sentence>> =
        suspendTransaction {
            val rows = SentencesTable
                .selectAll()
                .where { SentencesTable.textId eq textId.value.toKotlinUuid() }
                .orderBy(SentencesTable.position)
                .toList()

            if (rows.isEmpty()) return@suspendTransaction emptyList<Sentence>().right()

            val sentenceIds = rows.map { it[SentencesTable.id] }
            val tokensBySentenceId = loadTokensByKotlinUuid(sentenceIds)

            rows.map { row ->
                val sid = row[SentencesTable.id]
                row.toSentence(tokensBySentenceId[sid] ?: emptyList())
            }.right()
        }

    override suspend fun findById(id: SentenceId): Either<DomainError, Sentence> =
        suspendTransaction {
            val row = SentencesTable
                .selectAll()
                .where { SentencesTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?: return@suspendTransaction DomainError.NotFound("Sentence", id.toString()).left()

            val tokens = loadTokensByKotlinUuid(listOf(row[SentencesTable.id]))[row[SentencesTable.id]]
                ?: emptyList()
            row.toSentence(tokens).right()
        }

    override suspend fun save(sentence: Sentence): Either<DomainError, Sentence> =
        try {
            suspendTransaction {
                SentencesTable.insert {
                    it[id] = sentence.id.value.toKotlinUuid()
                    it[textId] = sentence.textId.value.toKotlinUuid()
                    it[position] = sentence.position
                    it[arabicText] = sentence.arabicText
                    it[transliteration] = sentence.transliteration
                    it[freeTranslation] = sentence.freeTranslation
                    it[notes] = sentence.notes
                    it[tokensValid] = sentence.tokensValid
                    it[createdAt] = sentence.createdAt
                    it[updatedAt] = sentence.updatedAt
                }
                insertTokens(sentence.tokens)
                sentence.right()
            }
        } catch (e: ExposedSQLException) {
            if (e.message?.contains("sentences_text_id_fkey") == true) {
                DomainError.NotFound("Text", sentence.textId.toString()).left()
            } else {
                throw e
            }
        }

    override suspend fun update(sentence: Sentence): Either<DomainError, Sentence> =
        suspendTransaction {
            either {
                val updatedCount = SentencesTable.update({ SentencesTable.id eq sentence.id.value.toKotlinUuid() }) {
                    it[textId] = sentence.textId.value.toKotlinUuid()
                    it[position] = sentence.position
                    it[arabicText] = sentence.arabicText
                    it[transliteration] = sentence.transliteration
                    it[freeTranslation] = sentence.freeTranslation
                    it[notes] = sentence.notes
                    it[tokensValid] = sentence.tokensValid
                    it[updatedAt] = sentence.updatedAt
                }
                ensure(updatedCount > 0) { DomainError.NotFound("Sentence", sentence.id.toString()) }

                // Replace tokens: delete all, re-insert.
                AlignmentTokensTable.deleteWhere { sentenceId eq sentence.id.value.toKotlinUuid() }
                insertTokens(sentence.tokens)

                val refreshed = SentencesTable
                    .selectAll()
                    .where { SentencesTable.id eq sentence.id.value.toKotlinUuid() }
                    .singleOrNull()
                ensureNotNull(refreshed) { DomainError.NotFound("Sentence", sentence.id.toString()) }

                val tokens = loadTokensByKotlinUuid(listOf(refreshed[SentencesTable.id]))[refreshed[SentencesTable.id]]
                    ?: emptyList()
                refreshed.toSentence(tokens)
            }
        }

    override suspend fun delete(id: SentenceId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = SentencesTable.deleteWhere { SentencesTable.id eq id.value.toKotlinUuid() }
                ensure(deleteCount > 0) { DomainError.NotFound("Sentence", id.toString()) }
            }
        }

    override suspend fun replaceTokens(
        sentenceId: SentenceId,
        tokens: List<AlignmentToken>,
    ): Either<DomainError, Sentence> =
        suspendTransaction {
            either {
                val row = SentencesTable
                    .selectAll()
                    .where { SentencesTable.id eq sentenceId.value.toKotlinUuid() }
                    .singleOrNull()
                ensureNotNull(row) { DomainError.NotFound("Sentence", sentenceId.toString()) }

                // Mark tokens as valid on the sentence row.
                SentencesTable.update({ SentencesTable.id eq sentenceId.value.toKotlinUuid() }) {
                    it[tokensValid] = true
                }

                // Delete existing tokens and re-insert.
                AlignmentTokensTable.deleteWhere { AlignmentTokensTable.sentenceId eq sentenceId.value.toKotlinUuid() }
                insertTokens(tokens)

                val freshRow = SentencesTable
                    .selectAll()
                    .where { SentencesTable.id eq sentenceId.value.toKotlinUuid() }
                    .single()

                val freshTokens = loadTokensByKotlinUuid(listOf(freshRow[SentencesTable.id]))[freshRow[SentencesTable.id]]
                    ?: emptyList()
                freshRow.toSentence(freshTokens)
            }
        }

    override suspend fun maxPosition(textId: TextId): Either<DomainError, Int> =
        suspendTransaction {
            val max = SentencesTable
                .selectAll()
                .where { SentencesTable.textId eq textId.value.toKotlinUuid() }
                .maxByOrNull { SentencesTable.position }
                ?.get(SentencesTable.position)
            (max ?: 0).right()
        }

    // --- private helpers ---

    private fun loadTokensByKotlinUuid(sentenceKotlinIds: List<KotlinUUID>): Map<KotlinUUID, List<AlignmentToken>> {
        if (sentenceKotlinIds.isEmpty()) return emptyMap()
        return AlignmentTokensTable
            .selectAll()
            .where { AlignmentTokensTable.sentenceId inList sentenceKotlinIds }
            .orderBy(AlignmentTokensTable.position)
            .groupBy(
                { it[AlignmentTokensTable.sentenceId] },
                { it.toAlignmentToken() },
            )
    }

    private fun loadTokens(sentenceIds: List<KotlinUUID>): Map<KotlinUUID, List<AlignmentToken>> =
        loadTokensByKotlinUuid(sentenceIds)

    private fun insertTokens(tokens: List<AlignmentToken>) {
        tokens.forEach { token ->
            AlignmentTokensTable.insert {
                it[id] = token.id.value.toKotlinUuid()
                it[sentenceId] = token.sentenceId.value.toKotlinUuid()
                it[position] = token.position
                it[arabic] = token.arabic
                it[transliteration] = token.transliteration
                it[translation] = token.translation
                it[wordId] = token.wordId?.value?.toKotlinUuid()
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun ResultRow.toSentence(tokens: List<AlignmentToken>) = Sentence(
    id = SentenceId(this[SentencesTable.id].toJavaUuid()),
    textId = TextId(this[SentencesTable.textId].toJavaUuid()),
    position = this[SentencesTable.position],
    arabicText = this[SentencesTable.arabicText],
    transliteration = this[SentencesTable.transliteration],
    freeTranslation = this[SentencesTable.freeTranslation],
    notes = this[SentencesTable.notes],
    tokensValid = this[SentencesTable.tokensValid],
    tokens = tokens,
    createdAt = this[SentencesTable.createdAt],
    updatedAt = this[SentencesTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
private fun ResultRow.toAlignmentToken() = AlignmentToken(
    id = AlignmentTokenId(this[AlignmentTokensTable.id].toJavaUuid()),
    sentenceId = SentenceId(this[AlignmentTokensTable.sentenceId].toJavaUuid()),
    position = this[AlignmentTokensTable.position],
    arabic = this[AlignmentTokensTable.arabic],
    transliteration = this[AlignmentTokensTable.transliteration],
    translation = this[AlignmentTokensTable.translation],
    wordId = this[AlignmentTokensTable.wordId]?.toJavaUuid()?.let { WordId(it) },
)
