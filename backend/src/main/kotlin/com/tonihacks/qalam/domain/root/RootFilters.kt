package com.tonihacks.qalam.domain.root

enum class RootSortField { UPDATED_AT, CREATED_AT, NORMALIZED_FORM, LETTER_COUNT }

data class RootFilters(
    val letterCount: Int? = null,
    val sortBy: RootSortField = RootSortField.UPDATED_AT,
    val sortDesc: Boolean = true,
)
