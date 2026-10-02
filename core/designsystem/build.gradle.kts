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
            // Las fuentes de Outfit y los iconos salen de composeResources.
            implementation(compose.components.resources)
            // Solo la anotación @Preview del catálogo de componentes.
            implementation(compose.preview)
        }
    }
}

compose.resources {
    // La clase Res no sale del módulo: fuera de aquí se usa el tema, no los recursos.
    publicResClass = false
    packageOfResClass = "app.reporrun.core.designsystem.resources"
}
