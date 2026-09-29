// Build incluido con los convention plugins de gymdi-app. Es un build aparte: no hereda
// los repositorios ni el catálogo del raíz, así que los declara aquí.
rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
