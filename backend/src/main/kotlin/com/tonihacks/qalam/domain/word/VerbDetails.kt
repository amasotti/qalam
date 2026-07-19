package com.tonihacks.qalam.domain.word

import kotlin.time.Instant

data class VerbDetails(
    val wordId: WordId,
    val verbForm: VerbPattern,
    val pastPattern: String?,
    val presentPattern: String?,
    val weaknessType: WeaknessType,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class WeaknessType {
    SOUND, ASSIMILATED, HOLLOW, GEMINATE, DEFECTIVE, DOUBLY_WEAK;

    companion object {
        fun fromString(value: String): WeaknessType? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
