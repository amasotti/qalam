plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.detekt)
}

repositories {
    mavenCentral()
}

//dependencies {
//
//}

group = "com.tonihacks"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    }
}
