import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de aplicación Android: solo el arranque (MainActivity, manifest, tema de
// ventana, R8). Toda la UI vive en :composeApp, que es una librería KMP.
// El motivo del reparto está en el README, sección "Decisiones de compilación".
// AGP 9 compila Kotlin por su cuenta: aplicar 'org.jetbrains.kotlin.android' aquí
// aborta la build ("no longer required for Kotlin support since AGP 9.0").
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    // No es KMP, así que no hereda la calidad de reporrun.kmp.library.
    alias(libs.plugins.reporrun.quality)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

android {
    namespace = "app.reporrun"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.reporrun"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    // setContent recibe un lambda @Composable: el runtime tiene que estar en el
    // classpath de compilación de este módulo, no solo en el de :composeApp.
    implementation(compose.runtime)
}
