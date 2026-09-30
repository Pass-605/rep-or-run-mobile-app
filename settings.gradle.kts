rootProject.name = "rep-or-run-mobile-app"

pluginManagement {
    // Convention plugins reporrun.* (ver build-logic/).
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Ningún módulo declara repositorios por su cuenta: si alguno lo intenta, la build falla.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":androidApp")
include(":composeApp")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:domain")
include(":core:datastore")
include(":core:data")
