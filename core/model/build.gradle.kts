plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: LocalDate y LocalTime forman parte de los tipos públicos del modelo.
            api(libs.kotlinx.datetime)
        }
    }
}
