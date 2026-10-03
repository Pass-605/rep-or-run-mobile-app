package app.reporrun.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Módulo `feature:*`: Compose + tests y las únicas dependencias que permite
 * `CLAUDE.md` §3 (`core:domain`, `core:model`, `core:designsystem`, `core:ui`).
 * Ningún feature debe añadir `core:data`, `core:database`, `core:network`, `core:ai`
 * ni otro `feature:*`; lo comprueba `checkModuleDependencies` ([ModuleDependencyRules]).
 *
 * Trae también lo que pide el armazón de S-09 para una pantalla: Koin (su `val xxxModule` y
 * `koinViewModel()`), Lifecycle (`ViewModel`, `collectAsStateWithLifecycle`), Navigation
 * Compose y el plugin de serialización para las rutas `@Serializable`.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("reporrun.kmp.compose")
        pluginManager.apply("reporrun.test")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:model"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
                screenLibraries.forEach { implementation(libs.findLibrary(it).get()) }
            }
        }
    }

    private companion object {
        val screenLibraries = listOf(
            "koin-core",
            "koin-compose",
            "koin-compose-viewmodel",
            "lifecycle-viewmodel-compose",
            "lifecycle-runtime-compose",
            "navigation-compose",
        )
    }
}
