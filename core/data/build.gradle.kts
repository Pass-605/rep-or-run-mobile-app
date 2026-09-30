plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
    alias(libs.plugins.kotlinSerialization)
}

/*
 * La semilla (`seed/equipment_catalog.json`, B-01) se empaqueta como fuente Kotlin generada:
 * un `String` con el JSON tal cual. Así no hace falta Compose ni un lector de archivos por
 * plataforma en la capa de datos, y Android, iOS y los tests leen exactamente lo mismo. El
 * archivo de `seed/` sigue siendo la única fuente: esto se regenera al cambiarlo.
 */
val seedCatalogFile: RegularFile = rootProject.layout.projectDirectory.file("seed/equipment_catalog.json")
val generateSeedCatalogSource by tasks.registering(GenerateSeedCatalogSource::class) {
    seedFile.set(seedCatalogFile)
    outputDirectory.set(layout.buildDirectory.dir("generated/seed/commonMain/kotlin"))
}

kotlin {
    // Los tests de repositorio corren en la JVM contra Room en memoria: el SQLite bundled de
    // la JVM trae nativo para el host, el de Android no (ver :core:database).
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSeedCatalogSource)
            dependencies {
                api(project(":core:domain"))
                implementation(project(":core:model"))
                implementation(project(":core:database"))
                implementation(project(":core:datastore"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

/** Escribe `SeedCatalogSource.kt` con el JSON de la semilla partido en literales cortos. */
@CacheableTask
abstract class GenerateSeedCatalogSource : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val seedFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val json = seedFile.get().asFile.readText(Charsets.UTF_8)
        // Un literal de cadena no puede pasar de 65 535 bytes en el pool de constantes de la
        // JVM: se trocea con margen de sobra, aunque hoy la semilla ocupa unos 18 KB.
        val chunks = json.chunked(CHUNK_LENGTH).joinToString(",\n") { "        \"${it.escaped()}\"" }
        val source = """
            |// Generado por la tarea generateSeedCatalogSource desde seed/equipment_catalog.json. No editar.
            |package app.reporrun.core.data.seed
            |
            |internal object SeedCatalogSource {
            |    /** El contenido de `seed/equipment_catalog.json`. */
            |    val json: String = listOf(
            |$chunks,
            |    ).joinToString(separator = "")
            |}
            |
        """.trimMargin()
        val directory = outputDirectory.get().asFile.resolve("app/reporrun/core/data/seed")
        directory.mkdirs()
        directory.resolve("SeedCatalogSource.kt").writeText(source, Charsets.UTF_8)
    }

    private fun String.escaped(): String = buildString {
        for (char in this@escaped) {
            when (char) {
                '\\' -> append("\\\\")

                '"' -> append("\\\"")

                '$' -> append("\\$")

                '\n' -> append("\\n")

                '\r' -> append("\\r")

                '\t' -> append("\\t")

                // Escapados, un par sustituto partido entre dos trozos sigue siendo válido.
                in '\uD800'..'\uDFFF' -> append("\\u%04X".format(char.code))

                else -> append(char)
            }
        }
    }

    private companion object {
        const val CHUNK_LENGTH = 4_000
    }
}
