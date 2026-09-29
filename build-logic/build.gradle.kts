plugins {
    `kotlin-dsl`
}

group = "app.gymdi.buildlogic"

// compileOnly: los plugins reales los pone en el classpath el build.gradle.kts raíz
// (con `apply false`); aquí solo hacen falta para compilar contra su API.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "gymdi.kmp.library"
            implementationClass = "app.gymdi.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "gymdi.kmp.compose"
            implementationClass = "app.gymdi.buildlogic.KmpComposeConventionPlugin"
        }
        register("feature") {
            id = "gymdi.feature"
            implementationClass = "app.gymdi.buildlogic.FeatureConventionPlugin"
        }
        register("test") {
            id = "gymdi.test"
            implementationClass = "app.gymdi.buildlogic.TestConventionPlugin"
        }
    }
}
