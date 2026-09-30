plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
}

kotlin {
    // :core:data corre sus tests en la JVM, y aquí los tests del DataStore usan un archivo
    // temporal del host.
    jvm()

    sourceSets {
        commonMain.dependencies {
            // `api`: el DataStore que se construye aquí lo inyecta el grafo de la app.
            api(libs.androidx.datastore.preferences.core)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
