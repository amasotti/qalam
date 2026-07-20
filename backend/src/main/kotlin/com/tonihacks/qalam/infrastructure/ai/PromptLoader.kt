package com.tonihacks.qalam.infrastructure.ai

object PromptLoader {
    private val placeholder = Regex("<([A-Za-z][A-Za-z0-9]*)>")

    fun loadPrompt(path: String, values: Map<String, String> = emptyMap()): String {
        val template = requireNotNull(PromptLoader::class.java.classLoader.getResourceAsStream(path)) {
            "Prompt resource not found: $path"
        }.bufferedReader().use { it.readText() }

        val requiredKeys = placeholder.findAll(template).map { it.groupValues[1] }.toSet()
        require(requiredKeys.all(values::containsKey)) {
            "Missing prompt values for $path: ${(requiredKeys - values.keys).sorted().joinToString()}"
        }

        return requiredKeys.fold(template) { rendered, key ->
            rendered.replace("<$key>", values.getValue(key))
        }.trimIndent()
    }
}
