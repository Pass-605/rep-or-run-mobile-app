plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
}

kotlin {
    // :core:data corre sus tests de repositorio en la JVM (SQLite bundled) y necesita que
    // todo lo que consume compile para ese target.
    jvm()

    sourceSets {
        commonMain.dependencies {
            // `api`: los repositorios exponen modelos y Flow en su firma pública.
            api(project(":core:model"))
            api(libs.kotlinx.coroutines.core)
            // Solo para `domainModule` (paquete `di`): los casos de uso no ven Koin.
            implementation(libs.koin.core)
        }
    }
}
