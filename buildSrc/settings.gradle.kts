import java.io.File

rootProject.name = "buildSrc"

// Configure repositories for plugin resolution
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}


pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Optional: Configure build cache for faster builds
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-src-cache")
    }
}
