plugins {
    alias(libs.plugins.reporrun.kmp.compose)
    alias(libs.plugins.reporrun.test)
}

kotlin {
    androidLibrary {
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            // api porque quien use el tema necesita los tipos de Material 3.
            api(compose.material3)
        }
    }
}
