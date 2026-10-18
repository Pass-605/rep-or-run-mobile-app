// UI compartida. Desde AGP 9 el plugin 'com.android.application' no puede convivir
// con el de Kotlin Multiplatform en el mismo módulo, así que este módulo es una
// librería KMP y el APK lo produce :androidApp. En iOS sigue siendo el framework.
//
// Es también la raíz de composición: aquí se arma el grafo de Koin y la navegación raíz,
// y por eso es el único módulo que ve a la vez el dominio y sus implementaciones.
plugins {
    alias(libs.plugins.reporrun.kmp.compose)
    alias(libs.plugins.reporrun.test)
    // Rutas de navegación @Serializable.
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "app.reporrun.shared"
        // Los textos de composeResources.
        androidResources { enable = true }
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
            implementation(project(":core:domain"))
            // Solo para armar el grafo: de estos cinco no se usa nada más que su módulo de Koin
            // y los builders de plataforma.
            implementation(project(":core:ai"))
            implementation(project(":core:data"))
            // La NetworkConfig del proxy: la versión de AppInfo y el installId solo se juntan aquí.
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:datastore"))
            // api: :androidApp y el framework de iOS entran por RepOrRunApp(), que
            // devuelve tipos del tema.
            api(project(":core:designsystem"))
            // Los features: de cada uno solo se usa su grafo de navegación y su módulo de Koin.
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:gym"))
            implementation(project(":feature:plansetup"))
            implementation(project(":feature:plan"))
            implementation(project(":feature:settings"))
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
        }
        // Module.verify() es API de JVM: el test del grafo corre en el host de Android.
        getByName("androidHostTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}

compose.resources {
    // Los textos no salen del módulo: cada feature tendrá los suyos.
    publicResClass = false
    packageOfResClass = "app.reporrun.resources"
}
