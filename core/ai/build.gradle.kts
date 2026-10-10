plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Los tests de contrato leen `contract/v1/examples/` del disco, y eso solo se puede hacer
    // en la JVM. :core:data, que consume este módulo, también necesita el target.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// La copia versionada del contrato (scripts/sync-contract.sh), nunca `../docs/`: el repo
// tiene que pasar sus tests sin el resto del workspace.
val contractDir: Directory = rootProject.layout.projectDirectory.dir("contract/v1")
tasks.named<Test>("jvmTest") {
    inputs.dir(contractDir).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("reporrun.contractDir", contractDir.asFile.absolutePath)
}
