plugins {
    alias(libs.plugins.reporrun.kmp.library)
    alias(libs.plugins.reporrun.test)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    // Solo para los tests de DAO: el driver bundled de la JVM trae SQLite nativo para
    // Windows, Linux y macOS, así que los tests corren en cualquier máquina de desarrollo y
    // en la CI. En los tests en host de Android no hay SQLite nativo del host.
    jvm()

    // Room genera el `actual` de RepOrRunDatabaseConstructor: sin esto, aviso de clases
    // expect/actual en beta en cada compilación.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            // `api`: las entidades usan los enums de :core:model y los tipos de fecha, y
            // :core:data las recibe tal cual.
            api(project(":core:model"))
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// El compilador de Room se engancha target a target: KSP en KMP no tiene `ksp` común.
dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room.compiler) }
}
