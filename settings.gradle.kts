pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    // downloads missing jdk for `jvmToolchain` see: https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.10.0") // https://github.com/gradle/foojay-toolchains/tags
}

rootProject.name = "localizer"
