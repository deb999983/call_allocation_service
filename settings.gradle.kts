rootProject.name = "call_allocation_service"

pluginManagement {
    repositories {
        gradlePluginPortal();
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.4.21"
        kotlin("plugin.serialization") version "2.4.21"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
    }
}


include("app")