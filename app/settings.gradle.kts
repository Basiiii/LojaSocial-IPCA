pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Add fallback repositories for Play Services
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        maven { url = uri("https://maven.google.com") }
    }
}

rootProject.name = "LojaSocial"
include(":app")