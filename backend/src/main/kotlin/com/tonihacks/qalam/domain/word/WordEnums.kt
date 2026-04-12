package com.tonihacks.qalam.domain.word

enum class PartOfSpeech {
    UNKNOWN, NOUN, VERB, ADJECTIVE, ADVERB,
    PREPOSITION, PARTICLE, INTERJECTION, CONJUNCTION, PRONOUN;

    companion object {
        fun fromString(value: String): PartOfSpeech? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

enum class Dialect {
    TUNISIAN, MOROCCAN, EGYPTIAN, GULF, LEVANTINE, MSA, IRAQI;

    companion object {
        fun fromString(value: String): Dialect? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED;

    companion object {
        fun fromString(value: String): Difficulty? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

enum class MasteryLevel {
    NEW, LEARNING, KNOWN, MASTERED;

    companion object {
        fun fromString(value: String): MasteryLevel? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

enum class DictionarySource {
    ALMANY, LIVING_ARABIC, DERJA_NINJA, REVERSO,
    WIKTIONARY, ARABIC_STUDENT_DICTIONARY, LANGENSCHEIDT, CUSTOM;

    companion object {
        fun fromString(value: String): DictionarySource? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
