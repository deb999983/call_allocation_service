import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.repositories

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.jib.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("jibWithLoadTar") {
            id = "no.cas.gradle.plugins.jib-jvm"
            implementationClass = "no.tta.plugins.JibJvmPlugin"
        }
    }
}
