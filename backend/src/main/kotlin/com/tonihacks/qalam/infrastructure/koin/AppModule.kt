package com.tonihacks.qalam.infrastructure.koin

import com.tonihacks.qalam.domain.annotation.AnnotationRepository
import com.tonihacks.qalam.domain.annotation.AnnotationService
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.domain.sentence.SentenceRepository
import com.tonihacks.qalam.domain.sentence.SentenceService
import com.tonihacks.qalam.domain.text.TextRepository
import com.tonihacks.qalam.domain.text.TextService
import com.tonihacks.qalam.domain.transliteration.TransliterationService
import com.tonihacks.qalam.domain.word.WordRepository
import com.tonihacks.qalam.domain.word.WordService
import com.tonihacks.qalam.infrastructure.ai.AiClient
import com.tonihacks.qalam.infrastructure.exposed.ExposedAnnotationRepository
import com.tonihacks.qalam.infrastructure.exposed.ExposedRootRepository
import com.tonihacks.qalam.infrastructure.exposed.ExposedSentenceRepository
import com.tonihacks.qalam.infrastructure.exposed.ExposedTextRepository
import com.tonihacks.qalam.infrastructure.exposed.ExposedWordRepository
import org.koin.dsl.module

val rootsModules = module {
    single<RootRepository> { ExposedRootRepository() }
    single { RootService(get()) }
}

val wordsModules = module {
    single { AiClient() }
    single<WordRepository> { ExposedWordRepository() }
    single { WordService(get(), get()) }
}

val textsModule = module {
    single<TextRepository> { ExposedTextRepository() }
    single { TextService(get()) }
}

val sentencesModule = module {
    single<SentenceRepository> { ExposedSentenceRepository() }
    single { SentenceService(get()) }
}

val transliterationModule = module {
    single { TransliterationService() }
}

val annotationsModule = module {
    single<AnnotationRepository> { ExposedAnnotationRepository() }
    single { AnnotationService(get()) }
}

val appModule = module {
    includes(rootsModules, wordsModules, textsModule, sentencesModule, transliterationModule, annotationsModule)
}
