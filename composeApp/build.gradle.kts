// UI compartida. Desde AGP 9 el plugin 'com.android.application' no puede convivir
// con el de Kotlin Multiplatform en el mismo módulo, así que este módulo es una
// librería KMP y el APK lo produce :androidApp. En iOS sigue siendo el framework.
plugins {
    alias(libs.plugins.gymdi.kmp.compose)
    alias(libs.plugins.gymdi.test)
}

kotlin {
    androidLibrary {
        namespace = "app.gymdi.shared"
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    compilerOptions {
        // Lo necesitan PdfRenderer y ReminderScheduler, que son expect/actual class.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            // api: :androidApp y el framework de iOS entran por GymdiApp(), que
            // devuelve tipos del tema.
            api(project(":core:designsystem"))
            implementation(compose.material3)
            implementation(compose.components.resources)
        }
    }
}
