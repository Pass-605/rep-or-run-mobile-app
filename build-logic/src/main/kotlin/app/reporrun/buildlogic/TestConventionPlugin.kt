package app.reporrun.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure

/**
 * Dependencias de test comunes en `commonTest`: `kotlin.test`, coroutines-test (`runTest`) y
 * Turbine, para los `Flow` de los ViewModels. Se aplica sobre un módulo KMP
 * (`reporrun.kmp.library` o derivados).
 *
 * Activa además los tests en host de Android (`androidHostTest`), que ejecutan
 * `commonTest` en la JVM: en Windows y Linux los targets de iOS no se compilan, así que
 * sin ellos `allTests` no correría ningún test.
 */
class TestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            kotlinMultiplatform {
                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                    implementation(libs.findLibrary("turbine").get())
                }
            }
        }
        pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            kotlinMultiplatform {
                val extensions = (this as ExtensionAware).extensions
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
                    withHostTest { }
                }
            }
        }
    }
}
