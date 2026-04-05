package com.tonihacks.qalam.infrastructure.koin

import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.infrastructure.exposed.ExposedRootRepository
import org.koin.dsl.module

val rootsModules = module {
    single<RootRepository> { ExposedRootRepository() }
    single { RootService(get()) }
}

val appModule = module {
    includes(rootsModules)
}
