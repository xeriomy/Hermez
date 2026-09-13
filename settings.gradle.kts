// Hermes Android Client - Project Settings
// This file is part of the Hermes Android project

// Plugin management for Android plugins
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "hermes-android"

// Include the app module
include(":app")
