package app.reporrun.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private const val JDK_TOOLCHAIN = 21

/**
 * Librería KMP con Android e iOS: el esqueleto común de todos los módulos de la app.
 *
 * Con AGP 9 una librería KMP usa `com.android.kotlin.multiplatform.library` y se
 * configura en `kotlin { androidLibrary { } }`; `com.android.library` no convive con KMP.
 * El namespace por defecto sale de la ruta (`:core:model` → `app.reporrun.core.model`);
 * el módulo puede sobrescribirlo en su propio `androidLibrary { }`.
 * Trae también detekt, Spotless y la regla de dependencias ([QualityConventionPlugin]).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("reporrun.quality")

        kotlinMultiplatform {
            // El proyecto se compila con JDK 21; el bytecode de Android va a 17, que es
            // hasta donde D8 llega sin sorpresas. Ver README.
            jvmToolchain(JDK_TOOLCHAIN)

            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
                namespace = "app.reporrun" + path.replace(':', '.')
                compileSdk = libs.intVersion("compileSdk")
                minSdk = libs.intVersion("minSdk")
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }

            iosArm64()
            iosSimulatorArm64()
        }
    }
}
