rootProject.name = "call_allocation_service"

pluginManagement {
    repositories {
        gradlePluginPortal();
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.4.21"
        id("com.google.cloud.tools.jib") version "3.5.4"
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