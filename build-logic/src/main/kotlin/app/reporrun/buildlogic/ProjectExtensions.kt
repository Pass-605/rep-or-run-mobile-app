package app.reporrun.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.intVersion(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

internal fun Project.kotlinMultiplatform(configure: KotlinMultiplatformExtension.() -> Unit) =
    extensions.configure(KotlinMultiplatformExtension::class.java, configure)
