
plugins {
    kotlin("jvm") version "1.6.20"
    `java-gradle-plugin`
    `maven-publish`
    // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

group = "ch.hippmann.gradle.plugin.localizer"
version = "0.0.1"

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

pluginBundle {
    website = "https://github.com/chippmann/localizer"
    vcsUrl = "https://github.com/chippmann/localizer.git"
    tags = listOf("kotlin", "android")
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

tasks {
    val sourceJar by creating(Jar::class) {
        archiveBaseName.set(project.name)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

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
            artifact(tasks.getByName("sourceJar"))
        }
    }
}
