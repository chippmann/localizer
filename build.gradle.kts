
plugins {
    kotlin("jvm") version "1.6.20"
    `java-gradle-plugin`
    `maven-publish`
    // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
    id("com.gradle.plugin-publish") version "0.18.0"
}

group = "ch.hippmann"
version = "0.0.1"

pluginBundle {
    website = "https://github.com/chippmann/localizer"
    vcsUrl = "https://github.com/chippmann/localizer.git"
    tags = listOf("kotlin", "android")

    mavenCoordinates {
        groupId = "${project.group}"
        artifactId = project.name
        version = "${project.version}"
    }
}

gradlePlugin {
    plugins {
        create("localizer") {
            id = "ch.hippmann.localizer"
            displayName = "Generate localization files for Android and KMP"
            description = "Gradle plugin for generating localization files for Android projects and small Kotlin Multiplatform projects"
            implementationClass = "ch.hippmann.localizer.plugin.LocalizerGradlePlugin"
        }
    }
    isAutomatedPublishing = false
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))

    val ktorVersion = "2.0.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    // https://github.com/square/kotlinpoet/releases
    implementation("com.squareup:kotlinpoet:1.11.0")
}

java {
    withSourcesJar()
}

tasks {
    build {
        finalizedBy(publishToMavenLocal)
    }
}

publishing {
    publications {
        // this is only used for publishing locally.
        val localizerPlugin by creating(MavenPublication::class) {
            pom {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
            }
            from(components.getByName("java"))
        }
    }
}
