package com.tonihacks.qalam.infrastructure.ai

object PromptLoader {

    fun loadPrompt(path: String): String = object {}.javaClass.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalArgumentException("Resource not found")

}
