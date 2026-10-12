package app.reporrun.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Empaqueta archivos JSON del repo como fuente Kotlin: un `internal object` con un `String`
 * por archivo. `commonMain` no puede leer del disco, y asi Android, iOS y los tests leen
 * exactamente lo mismo sin un lector de archivos por plataforma. Los archivos siguen siendo
 * la unica fuente: esto se regenera al cambiarlos.
 *
 * Cada propiedad se llama como su archivo en camelCase: `generate-response.small-gym.json`
 * es `generateResponseSmallGym`.
 */
@CacheableTask
abstract class GenerateJsonSourceTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val jsonFiles: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val objectName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val files = jsonFiles.files.sortedBy { it.name }
        val source = JsonSource.render(
            packageName = packageName.get(),
            objectName = objectName.get(),
            taskName = name,
            jsonByFileName = files.associate { it.name to it.readText(Charsets.UTF_8) },
        )
        val directory = outputDirectory.get().asFile.resolve(packageName.get().replace('.', '/'))
        directory.mkdirs()
        directory.resolve("${objectName.get()}.kt").writeText(source, Charsets.UTF_8)
    }
}

/** El texto que escribe [GenerateJsonSourceTask], aparte para poder probarlo sin Gradle. */
internal object JsonSource {
    // Un literal de cadena no puede pasar de 65 535 bytes en el pool de constantes de la JVM:
    // se trocea con margen de sobra.
    private const val CHUNK_LENGTH = 4_000

    fun render(packageName: String, objectName: String, taskName: String, jsonByFileName: Map<String, String>): String =
        buildString {
            appendLine("// Generado por la tarea $taskName. No editar.")
            appendLine("package $packageName")
            appendLine()
            appendLine("internal object $objectName {")
            jsonByFileName.entries.forEachIndexed { index, (fileName, json) ->
                if (index > 0) appendLine()
                appendLine("    /** El contenido de `$fileName`. */")
                appendLine("    val ${propertyName(fileName)}: String = listOf(")
                json.chunked(CHUNK_LENGTH).forEach { appendLine("        \"${escape(it)}\",") }
                appendLine("    ).joinToString(separator = \"\")")
            }
            appendLine("}")
        }

    /** `generate-response.small-gym.json` es `generateResponseSmallGym`. */
    fun propertyName(fileName: String): String {
        val words = fileName.removeSuffix(".json").split('-', '.', '_').filter { it.isNotEmpty() }
        return words.first() + words.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    fun escape(text: String): String = buildString {
        for (char in text) {
            when (char) {
                '\\' -> append("\\\\")

                '"' -> append("\\\"")

                '$' -> append("\\$")

                '\n' -> append("\\n")

                '\r' -> append("\\r")

                '\t' -> append("\\t")

                // Escapados, un par sustituto partido entre dos trozos sigue siendo valido.
                in '\uD800'..'\uDFFF' -> append("\\u%04X".format(char.code))

                else -> append(char)
            }
        }
    }
}
