package app.reporrun.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Módulo `feature:*`: Compose + tests y las únicas dependencias que permite
 * `CLAUDE.md` §3 (`core:domain`, `core:model`, `core:designsystem`, `core:ui`).
 * Ningún feature debe añadir `core:data`, `core:database`, `core:network`, `core:ai`
 * ni otro `feature:*`; lo comprueba `checkModuleDependencies` ([ModuleDependencyRules]).
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("reporrun.kmp.compose")
        pluginManager.apply("reporrun.test")

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:model"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
            }
        }
    }
}
