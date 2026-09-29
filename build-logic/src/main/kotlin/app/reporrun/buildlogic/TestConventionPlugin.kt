package app.reporrun.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Dependencias de test comunes en `commonTest`. Se aplica sobre un módulo KMP
 * (`reporrun.kmp.library` o derivados); aquí entrarán Turbine y coroutines-test cuando
 * estén en el catálogo.
 */
class TestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            kotlinMultiplatform {
                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
