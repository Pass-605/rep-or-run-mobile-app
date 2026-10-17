import app.reporrun.buildlogic.GenerateLocalConfigSourceTask
import app.reporrun.buildlogic.localConfigValues

plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
    // Solo para los tests: comprueban ContentNegotiation con un cuerpo @Serializable.
    alias(libs.plugins.kotlinSerialization)
}

/*
 * La clave de app y las URL base salen de local.properties (o del entorno, con el mismo
 * nombre), nunca del repo. Si falta alguna, este módulo no compila y el error dice cuál:
 * mejor eso que un 401 en runtime que parece otra cosa. Ver local.properties.example.
 */
val localConfigNames = listOf("ROR_APP_KEY", "ROR_BASE_URL_ANDROID", "ROR_BASE_URL_IOS")

val generateNetworkBuildConfig by tasks.registering(GenerateLocalConfigSourceTask::class) {
    requiredNames.set(localConfigNames)
    values.set(localConfigValues(localConfigNames))
    packageName.set("app.reporrun.core.network")
    objectName.set("NetworkBuildConfig")
    outputDirectory.set(layout.buildDirectory.dir("generated/localConfig/commonMain/kotlin"))
}

kotlin {
    // :core:ai, que consume este módulo, tiene target JVM para sus tests de contrato:
    // sin él aquí, su jvmMain no resuelve la dependencia.
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateNetworkBuildConfig)
            dependencies {
                // `api`: createHttpClient devuelve un HttpClient y recibe un Json.
                api(libs.ktor.client.core)
                api(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        jvmMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        commonTest.dependencies { implementation(libs.ktor.client.mock) }
    }
}
