plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
}

kotlin {
    // :core:database lo necesita para sus tests de DAO en la JVM; el modelo es Kotlin puro y
    // compila igual para ese target.
    jvm()

    sourceSets {
        commonMain.dependencies {
            // `api`: LocalDate y LocalTime forman parte de los tipos públicos del modelo.
            api(libs.kotlinx.datetime)
        }
    }
}
