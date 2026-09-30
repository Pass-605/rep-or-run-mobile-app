plugins {
    `kotlin-dsl`
}

group = "app.reporrun.buildlogic"

// compileOnly: los plugins reales los pone en el classpath el build.gradle.kts raíz
// (con `apply false`); aquí solo hacen falta para compilar contra su API.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)

    // La regla de dependencias entre módulos se prueba con grafos de ejemplo.
    testImplementation(kotlin("test-junit"))
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "reporrun.kmp.library"
            implementationClass = "app.reporrun.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "reporrun.kmp.compose"
            implementationClass = "app.reporrun.buildlogic.KmpComposeConventionPlugin"
        }
        register("feature") {
            id = "reporrun.feature"
            implementationClass = "app.reporrun.buildlogic.FeatureConventionPlugin"
        }
        register("test") {
            id = "reporrun.test"
            implementationClass = "app.reporrun.buildlogic.TestConventionPlugin"
        }
        register("quality") {
            id = "reporrun.quality"
            implementationClass = "app.reporrun.buildlogic.QualityConventionPlugin"
        }
        register("root") {
            id = "reporrun.root"
            implementationClass = "app.reporrun.buildlogic.RootConventionPlugin"
        }
    }
}
