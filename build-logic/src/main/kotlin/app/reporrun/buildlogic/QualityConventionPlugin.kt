package app.reporrun.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * detekt, Spotless (ktlint) y [CHECK_MODULE_DEPENDENCIES] para un módulo. Lo aplica
 * [KmpLibraryConventionPlugin]; `:androidApp`, que no es KMP, lo declara a mano.
 * Los tres quedan colgados de `check`.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureDetekt(sources = listOf("src"))
        configureSpotless(
            kotlinTargets = listOf("src/**/*.kt"),
            gradleTargets = listOf("*.gradle.kts"),
        )
        registerModuleDependencyCheck()
    }
}
