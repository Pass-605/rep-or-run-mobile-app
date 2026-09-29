package app.gymdi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension

/**
 * [KmpLibraryConventionPlugin] más Compose Multiplatform, con runtime y foundation en
 * `commonMain`. Material 3, recursos y demás los declara cada módulo según los use.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("gymdi.kmp.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        // Los accesores `compose.*` están deprecados en CMP 1.12, pero son los que usan
        // hoy los módulos; pasar a coordenadas explícitas es un cambio aparte.
        @Suppress("DEPRECATION")
        val compose = extensions.getByType<ComposeExtension>().dependencies
        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                @Suppress("DEPRECATION")
                implementation(compose.runtime)
                @Suppress("DEPRECATION")
                implementation(compose.foundation)
            }
        }
    }
}
