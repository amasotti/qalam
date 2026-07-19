package com.tonihacks.qalam.domain.conjugation.model

/**
 * 13 person/number/gender combinations used in Arabic verb conjugation.
 * Labels follow standard Arabic grammatical terminology.
 */
enum class Person(val code: String, val arabicLabel: String, val translitLabel: String) {
    FIRST_SINGULAR("1S", "أَنَا", "anā"),
    SECOND_SINGULAR_MASC("2SM", "أَنْتَ", "anta"),
    SECOND_SINGULAR_FEM("2SF", "أَنْتِ", "anti"),
    THIRD_SINGULAR_MASC("3SM", "هُوَ", "huwa"),
    THIRD_SINGULAR_FEM("3SF", "هِيَ", "hiya"),
    SECOND_DUAL("2D", "أَنْتُمَا", "antumā"),
    THIRD_DUAL_MASC("3DM", "هُمَا", "humā (m.)"),
    THIRD_DUAL_FEM("3DF", "هُمَا", "humā (f.)"),
    FIRST_PLURAL("1P", "نَحْنُ", "naḥnu"),
    SECOND_PLURAL_MASC("2PM", "أَنْتُمْ", "antum"),
    SECOND_PLURAL_FEM("2PF", "أَنْتُنَّ", "antunna"),
    THIRD_PLURAL_MASC("3PM", "هُمْ", "hum"),
    THIRD_PLURAL_FEM("3PF", "هُنَّ", "hunna"),
}
