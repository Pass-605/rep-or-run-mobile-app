// Utilidades de UI que comparten los features y que no son diseño: lo visual va en
// :core:designsystem. No depende de ningún otro módulo del proyecto.
plugins {
    alias(libs.plugins.reporrun.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.lifecycle.runtime.compose)
        }
    }
}
