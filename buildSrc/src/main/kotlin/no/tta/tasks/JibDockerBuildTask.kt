package no.tta.tasks

import com.google.cloud.tools.jib.gradle.BuildTarTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class JibDockerBuildTask @Inject constructor(
    private val execOperations: ExecOperations
): DefaultTask() {
    @InputFiles
    val tarImagePath: ConfigurableFileCollection = project.files(project.tasks.named<BuildTarTask>("jibBuildTar"))

    @get:OutputFile
    val loadedImageIdFile: RegularFileProperty = project.objects.fileProperty().convention(project.layout.buildDirectory.file("docker/${project.name}-image.id"))

    private val imageRef = "${project.name}:v1.0.0"

    init {
        outputs.upToDateWhen {
            val recordedId = loadedImageIdFile.get().asFile.takeIf { it.exists() }?.readText()?.trim()
            !recordedId.isNullOrEmpty() && recordedId == daemonImageId(imageRef)
        }
    }

    @TaskAction
    fun build() {
        execOperations.exec {
            commandLine("docker", "load", "--input", tarImagePath.singleFile.absolutePath)
        }

        val loadedId = daemonImageId(imageRef) ?: error("Image $imageRef not found in the docker daemon after load")
        loadedImageIdFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(loadedId)
        }
    }

    private fun daemonImageId(imageRef: String): String? {
        val out = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine("docker", "image", "inspect", "--format", "{{.Id}}", imageRef)
            standardOutput = out
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return if (result.exitValue == 0) out.toString(Charsets.UTF_8).trim() else null
    }
}

