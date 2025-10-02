pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Яндекс MapKit
        maven { url = uri("https://maven.google.com") }
    }
}

rootProject.name = "xtrack"
include(":app")

