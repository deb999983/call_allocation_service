package no.tta.plugins

import com.google.cloud.tools.jib.gradle.BuildDockerTask
import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.google.cloud.tools.jib.gradle.BuildTarTask
import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibPlugin
import no.tta.tasks.JibDockerBuildTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register


abstract class JibBasePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // Apply the Jib plugin if not already applied
        if (!project.plugins.hasPlugin(JibPlugin::class.java)) {
            project.plugins.apply(JibPlugin::class.java)
        }

        // Jib's tasks hold a Project reference and call Task.project at execution time, which the configuration
        // cache forbids. Marking them lets Gradle fall back to a non-cached run instead of failing the build.
        listOf(BuildTarTask::class, BuildDockerTask::class, BuildImageTask::class).forEach { type ->
            project.tasks.withType(type.java).configureEach {
                notCompatibleWithConfigurationCache("Jib tasks access Task.project at execution time")
            }
        }

        // Configure Jib after the project is evaluated
        project.afterEvaluate {
            configureJibDefaults(project)
            configureJib(project)
        }

        // register dockerBuild task
        project.tasks.register<JibDockerBuildTask>("dockerBuild") {
            group = "docker"
            imageRef.set(project.provider {
                val to = project.extensions.getByType<JibExtension>().to
                "${to.image}:${to.tags.first()}"
            })
        }
    }

    abstract fun configureJib(project: Project)

    open fun configureJibDefaults(project: Project) {
        project.configure<JibExtension> {
            from {
                platforms {
                    platform {
                        architecture = "arm64"
                        os = "linux"
                    }
                }
            }
            // Only fill in what the build script left unset, so `jib { to { image = ... } }` wins
            to {
                if (image.isNullOrEmpty()) image = project.name
                if (tags.isNullOrEmpty()) tags = setOf("v1.0.0")
            }
        }
    }
}


open class JibJvmPlugin : JibBasePlugin() {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        project.pluginManager.apply(ApplicationPlugin::class.java)
        super.apply(project)
    }

    override fun configureJib(project: Project) {
        project.configure<JibExtension> {
            from {
                image = "amazoncorretto:25-jdk"
            }
        }
    }
}
