plugins {
    alias(libs.plugins.gymdi.kmp.compose)
    alias(libs.plugins.gymdi.test)
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
