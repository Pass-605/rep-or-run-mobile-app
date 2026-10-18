import app.reporrun.buildlogic.GenerateJsonSourceTask

plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
    alias(libs.plugins.kotlinSerialization)
}

// La copia versionada del contrato (scripts/sync-contract.sh), nunca `../docs/`: el repo
// tiene que pasar sus tests sin el resto del workspace.
val contractDir: Directory = rootProject.layout.projectDirectory.dir("contract/v1")

/*
 * Los fakes devuelven los ejemplos del contrato, y `commonMain` no lee archivos: los cuatro
 * que usan se empaquetan como fuente Kotlin generada. `contract/v1/examples/` sigue siendo la
 * única fuente; esto se regenera al sincronizar el contrato.
 */
val generateContractExamplesSource by tasks.registering(GenerateJsonSourceTask::class) {
    jsonFiles.from(
        contractDir.file("examples/generate-request.small-gym.json"),
        contractDir.file("examples/generate-response.small-gym.json"),
        contractDir.file("examples/review-response.sufficient.json"),
        contractDir.file("examples/review-response.insufficient.json"),
    )
    packageName.set("app.reporrun.core.ai.fake")
    objectName.set("ContractExampleSources")
    outputDirectory.set(layout.buildDirectory.dir("generated/contractExamples/commonMain/kotlin"))
}

kotlin {
    // Los tests de contrato leen `contract/v1/examples/` del disco, y eso solo se puede hacer
    // en la JVM. :core:data, que consume este módulo, también necesita el target.
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateContractExamplesSource)
            dependencies {
                // `api`: los fakes se registran por su port de :core:domain.
                api(project(":core:domain"))
                implementation(project(":core:model"))
                // El cliente HTTP de A-12; el proxy es lo único de aquí que habla por red.
                implementation(project(":core:network"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.core)
            }
        }
        commonTest.dependencies { implementation(libs.ktor.client.mock) }
    }
}

tasks.named<Test>("jvmTest") {
    inputs.dir(contractDir).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("reporrun.contractDir", contractDir.asFile.absolutePath)
}
