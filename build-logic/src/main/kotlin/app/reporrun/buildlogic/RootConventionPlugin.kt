package app.reporrun.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Proyecto raíz: detekt y Spotless sobre los scripts de la raíz y sobre `build-logic/`,
 * que como build incluido no recibe los convention plugins que define.
 *
 * El [CHECK_MODULE_DEPENDENCIES] de la raíz ejecuta los tests de `build-logic`, que
 * prueban la regla con grafos de ejemplo: así el comando no pasa en verde solo porque
 * todavía no exista ningún `feature:*`.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        check(this == rootProject) { "reporrun.root solo se aplica en el proyecto raíz" }
        pluginManager.apply("base")

        configureDetekt(sources = listOf("build-logic/src"))
        configureSpotless(
            kotlinTargets = listOf("build-logic/src/**/*.kt"),
            gradleTargets = listOf("*.gradle.kts", "build-logic/*.gradle.kts"),
        )

        val buildLogicTests = gradle.includedBuild("build-logic").task(":test")
        val checkTask = tasks.register(CHECK_MODULE_DEPENDENCIES) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Prueba la regla de dependencias entre módulos con grafos de ejemplo."
            dependsOn(buildLogicTests)
        }
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(checkTask) }
        Unit
    }
}
