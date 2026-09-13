// Hermes Android Client - Root Build File
// This file is part of the Hermes Android project

// Plugin management for version catalog
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.5.4" apply false
}

tasks.named("clean") {
    doLast {
        println("Cleaning Hermes Android project")
    }
}
