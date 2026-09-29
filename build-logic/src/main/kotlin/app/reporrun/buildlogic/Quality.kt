package app.reporrun.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * detekt sin type resolution sobre [sources] (rutas relativas al proyecto), con
 * `config/detekt/detekt.yml` encima de la configuración por defecto.
 */
internal fun Project.configureDetekt(sources: List<String>) {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom(sources)
        parallel.set(true)
    }
}

/**
 * Spotless con ktlint para Kotlin y scripts de Gradle. ktlint lee el `.editorconfig`
 * de la raíz, que es donde viven el estilo y la longitud de línea.
 *
 * La excepción de los `@Composable` (PascalCase, como los tipos) va aquí y no en el
 * `.editorconfig`: puesta allí, el ktlint de Spotless la ignora.
 */
internal fun Project.configureSpotless(kotlinTargets: List<String>, gradleTargets: List<String>) {
    pluginManager.apply("com.diffplug.spotless")
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    val editorConfig = rootProject.file(".editorconfig")
    val overrides = mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target(kotlinTargets)
            targetExclude("**/build/**")
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig).editorConfigOverride(overrides)
        }
        kotlinGradle {
            target(gradleTargets)
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig).editorConfigOverride(overrides)
        }
    }
}
