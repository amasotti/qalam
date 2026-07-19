package com.tonihacks.qalam.domain.conjugation

import com.tonihacks.qalam.domain.conjugation.model.ConjugationTable
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType

/**
 * Strategy interface for verb conjugation engines.
 * One implementation per dialect (MSA, Tunisian, …).
 */
interface ConjugationEngine {

    /**
     * Produce a full conjugation table for a verb.
     *
     * @param rootLetters triliteral root consonants, e.g. ["ك", "ت", "ب"]
     * @param verbForm Form I–X
     * @param pastPattern Form I vowel pattern, e.g. "fa3ala" (null for II–X)
     * @param presentPattern Form I vowel pattern, e.g. "yaf3ulu" (null for II–X)
     * @param weaknessType morphological weakness classification
     */
    fun conjugate(
        rootLetters: List<String>,
        verbForm: VerbPattern,
        pastPattern: String?,
        presentPattern: String?,
        weaknessType: WeaknessType,
    ): ConjugationTable
}
