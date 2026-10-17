package app.reporrun.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.StringReader
import java.util.Properties

private const val LOCAL_PROPERTIES = "local.properties"

/**
 * Escribe valores de `local.properties` (o del entorno) como constantes de un `internal object`:
 * lo que cambia de una maquina a otra entra en el codigo sin estar en git. Cada constante se
 * llama igual que su clave, para que haya un solo nombre en todas partes.
 *
 * Falla al ejecutarse, no al configurar, si falta alguna de [requiredNames]: solo rompe a quien
 * compila el modulo que la usa, y lo hace diciendo que falta en vez de dejarlo para runtime.
 *
 * No es `@CacheableTask` a proposito: los valores no tienen por que acabar en una cache de build,
 * y generar el archivo no cuesta nada.
 */
abstract class GenerateLocalConfigSourceTask : DefaultTask() {
    @get:Input
    abstract val requiredNames: ListProperty<String>

    /** Los valores encontrados, por nombre. Normalmente [localConfigValues]. */
    @get:Input
    abstract val values: MapProperty<String, String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val objectName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val found = values.get()
        val missing = LocalConfig.missing(requiredNames.get(), found)
        if (missing.isNotEmpty()) throw GradleException(LocalConfig.missingMessage(path, missing))

        val source = LocalConfig.render(
            packageName = packageName.get(),
            objectName = objectName.get(),
            taskName = name,
            values = found.filterKeys { it in requiredNames.get() },
        )
        val directory = outputDirectory.get().asFile.resolve(packageName.get().replace('.', '/'))
        directory.mkdirs()
        directory.resolve("${objectName.get()}.kt").writeText(source, Charsets.UTF_8)
    }
}

/**
 * Los valores de [names] que hay en el `local.properties` de la raiz o, si alli faltan, en la
 * variable de entorno del mismo nombre. Los que no estan en ningun sitio no aparecen.
 */
fun Project.localConfigValues(names: List<String>): Provider<Map<String, String>> {
    val propertiesText = providers
        .fileContents(rootProject.layout.projectDirectory.file(LOCAL_PROPERTIES))
        .asText
        .orElse("")
    val environment = names.fold(providers.provider { emptyMap<String, String>() }) { acc, name ->
        acc.zip(providers.environmentVariable(name).orElse("")) { map, value -> map + (name to value) }
    }
    return propertiesText.zip(environment) { text, env -> LocalConfig.resolve(names, text, env) }
}

/** La logica de [GenerateLocalConfigSourceTask], aparte para poder probarla sin Gradle. */
internal object LocalConfig {
    /** Manda `local.properties`; el entorno solo cubre lo que alli falta o esta en blanco. */
    fun resolve(names: List<String>, propertiesText: String, environment: Map<String, String>): Map<String, String> {
        val properties = Properties().apply { load(StringReader(propertiesText)) }
        return names
            .mapNotNull { name ->
                val value = properties.getProperty(name).nonBlank() ?: environment[name].nonBlank()
                value?.let { name to it }
            }.toMap()
    }

    fun missing(required: List<String>, values: Map<String, String>): List<String> = required.filter { it !in values }

    fun missingMessage(taskPath: String, missing: List<String>): String {
        val (verb, put, where) = if (missing.size == 1) {
            Triple("falta", "Ponla", "una variable de entorno")
        } else {
            Triple("faltan", "Ponlas", "variables de entorno")
        }
        return "$taskPath: $verb ${missing.joinToString()}. $put en $LOCAL_PROPERTIES (copia " +
            "$LOCAL_PROPERTIES.example) o en $where con el mismo nombre."
    }

    fun render(packageName: String, objectName: String, taskName: String, values: Map<String, String>): String =
        buildString {
            appendLine("// Generado por la tarea $taskName desde $LOCAL_PROPERTIES. No editar.")
            appendLine("package $packageName")
            appendLine()
            appendLine("internal object $objectName {")
            values.toSortedMap().forEach { (name, value) ->
                appendLine("    const val $name: String = \"${JsonSource.escape(value)}\"")
            }
            appendLine("}")
        }

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
