// Ajustes y licencias. Las dependencias de proyecto las pone `reporrun.feature`
// y son las únicas que CLAUDE.md §3 permite a un feature.
plugins {
    alias(libs.plugins.reporrun.feature)
}

kotlin {
    androidLibrary {
        // Los textos de composeResources.
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    // Los textos del feature no salen de él.
    publicResClass = false
    packageOfResClass = "app.reporrun.feature.settings.resources"
}
