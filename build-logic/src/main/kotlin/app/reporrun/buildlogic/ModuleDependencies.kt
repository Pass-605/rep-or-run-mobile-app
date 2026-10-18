package app.reporrun.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal const val CHECK_MODULE_DEPENDENCIES = "checkModuleDependencies"

/**
 * Reglas de dependencia entre modulos de `CLAUDE.md` seccion 3.
 *
 * La seccion 3 esta redactada como **lista de permitidos**, no de prohibidos, y asi se
 * implementa: si un modulo con regla depende de algo que no esta en su lista, falla. Eso
 * cubre tanto el caso obvio (un `feature:*` tirando de `core:data`) como el que se cuela
 * sin querer (`core:domain` dependiendo de cualquier cosa que no sea `core:model`), que
 * es la regla que sostiene la arquitectura.
 */
internal object ModuleDependencyRules {
    /** Clave para cualquier `:feature:*`: todos comparten regla. */
    private const val FEATURES = ":feature:*"

    private val allowed: Map<String, Set<String>> = mapOf(
        FEATURES to setOf(":core:domain", ":core:model", ":core:designsystem", ":core:ui"),
        ":core:data" to setOf(":core:domain", ":core:model", ":core:database", ":core:datastore", ":core:ai"),
        ":core:ai" to setOf(":core:network", ":core:domain", ":core:model"),
        // Fontaneria HTTP: no sabe que existe un plan. Traducir NetworkError a AiError es de :core:ai.
        ":core:network" to emptySet(),
        ":core:domain" to setOf(":core:model"),
        ":core:model" to emptySet(),
        // Lo visual no sabe de dominio: lo que un componente necesite decir le llega ya como texto.
        ":core:designsystem" to emptySet(),
    )

    /**
     * Dependencias permitidas para [module], o `null` si la seccion 3 no lo restringe. Un
     * modulo sin regla no se comprueba: prohibir por omision rompe la build con cada
     * modulo nuevo, y la fuente de verdad es el documento, no esta tabla.
     */
    fun allowedFor(module: String): Set<String>? = allowed[if (module.isFeature()) FEATURES else module]

    /** Dependencias directas de [module] que la regla no permite, ordenadas. */
    fun violations(module: String, dependencies: Set<String>): List<String> {
        val allowedHere = allowedFor(module) ?: return emptyList()
        return dependencies.filter { it != module && it !in allowedHere }.sorted()
    }

    private fun String.isFeature() = startsWith(":feature:")
}

/** Falla si las dependencias de proyecto declaradas en el modulo rompen [ModuleDependencyRules]. */
internal abstract class CheckModuleDependenciesTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val projectDependencies: SetProperty<String>

    @TaskAction
    fun check() {
        val module = modulePath.get()
        val violations = ModuleDependencyRules.violations(module, projectDependencies.get())
        if (violations.isEmpty()) return
        val allowedHere = ModuleDependencyRules.allowedFor(module).orEmpty()
        val canDependOn = if (allowedHere.isEmpty()) {
            "no puede depender de ningun modulo"
        } else {
            "solo puede depender de " + allowedHere.sorted().joinToString()
        }
        throw GradleException(
            "$module depende de ${violations.joinToString()}. Segun CLAUDE.md seccion 3, $canDependOn. " +
                "Si la dependencia es legitima, cambia primero el documento y luego esta tabla.",
        )
    }
}

/**
 * Registra [CHECK_MODULE_DEPENDENCIES] en el modulo y lo cuelga de `check`. Mira las
 * dependencias de proyecto directas de todas las configuraciones, tests incluidos: si
 * algun dia hay un modulo de utilidades de test, habra que anadirlo a las listas de
 * permitidos o excluir aqui las configuraciones de test.
 */
internal fun Project.registerModuleDependencyCheck() {
    // Fuera del bloque de la tarea: dentro, `path` seria la ruta de la tarea.
    val module = path
    val checkTask = tasks.register(CHECK_MODULE_DEPENDENCIES, CheckModuleDependenciesTask::class.java) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Comprueba las dependencias entre modulos de CLAUDE.md seccion 3."
        modulePath.set(module)
        projectDependencies.set(
            provider {
                configurations
                    .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                    .map { it.path }
                    .toSet()
            },
        )
    }
    plugins.withType(LifecycleBasePlugin::class.java) {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(checkTask) }
    }
}
