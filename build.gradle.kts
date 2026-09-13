// Hermes Android Client - Root Build File
// This file is part of the Hermes Android project

// Plugin management for version catalog
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.5.4" apply false
}

// Apply plugins in the plugins block to avoid resolution issues
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

tasks.named("clean") {
    doLast {
        println("Cleaning Hermes Android project")
    }
}
