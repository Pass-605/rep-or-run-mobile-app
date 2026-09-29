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
 * Reglas de dependencia entre módulos de `CLAUDE.md` §3. Por ahora solo la de los
 * `feature:*`, que es la que un descuido rompe con más facilidad.
 */
internal object ModuleDependencyRules {
    private val forbiddenForFeatures = setOf(":core:data", ":core:database", ":core:network", ":core:ai")

    /** Dependencias directas de [module] que la regla no permite, ordenadas. */
    fun violations(module: String, dependencies: Set<String>): List<String> {
        if (!module.isFeature()) return emptyList()
        return dependencies
            .filter { it in forbiddenForFeatures || (it.isFeature() && it != module) }
            .sorted()
    }

    private fun String.isFeature() = startsWith(":feature:")
}

/** Falla si las dependencias de proyecto declaradas en el módulo rompen [ModuleDependencyRules]. */
internal abstract class CheckModuleDependenciesTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val projectDependencies: SetProperty<String>

    @TaskAction
    fun check() {
        val module = modulePath.get()
        val violations = ModuleDependencyRules.violations(module, projectDependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "$module depende de ${violations.joinToString()}, y un feature:* no puede depender de " +
                    "core:data, core:database, core:network, core:ai ni de otro feature:* (CLAUDE.md §3).",
            )
        }
    }
}

/**
 * Registra [CHECK_MODULE_DEPENDENCIES] en el módulo y lo cuelga de `check`. Mira las
 * dependencias de proyecto directas de todas las configuraciones, tests incluidos.
 */
internal fun Project.registerModuleDependencyCheck() {
    // Fuera del bloque de la tarea: dentro, `path` sería la ruta de la tarea.
    val module = path
    val checkTask = tasks.register(CHECK_MODULE_DEPENDENCIES, CheckModuleDependenciesTask::class.java) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Comprueba las dependencias entre módulos de CLAUDE.md §3."
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
