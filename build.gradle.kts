import org.ajoberstar.grgit.Grgit

plugins {
    kotlin("jvm") version "1.6.20"
    `java-gradle-plugin`
    `maven-publish`
    // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
    id("com.gradle.plugin-publish") version "0.18.0"
    // https://github.com/ajoberstar/grgit/releases
    id("org.ajoberstar.grgit") version "5.0.0"
}

group = "ch.hippmann"
version = "1.0.1"

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
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    // https://github.com/square/kotlinpoet/releases
    implementation("com.squareup:kotlinpoet:1.11.0")
}

java {
    withSourcesJar()
}

val grGit: Grgit = Grgit.open(mapOf("currentDir" to project.rootDir))

tasks {
    build {
        finalizedBy(publishToMavenLocal)
    }

    @Suppress("UNUSED_VARIABLE") // used by github actions
    val generateChangelog by creating {
        group = "localizer"

        doLast {
            val tags = grGit.tag.list().reversed()
            println("INFO: Got ${tags.size} tags")

            val fromCommit = tags
                .getOrNull(1)
                ?.commit
                ?.id
                ?: grGit.log().last().id
            val toCommit = tags
                .getOrNull(0)
                ?.commit
                ?.id
                ?.let { commitId ->
                    if (commitId != fromCommit) {
                        commitId
                    } else {
                        grGit.log().first().id
                    }
                } ?: grGit.log().first().id

            println("INFO: Generate changelog; fromCommit: $fromCommit, toCommit: $toCommit")

            val changelogString = grGit
                .log {
                    range(fromCommit, toCommit)
                }
                .also {
                    println("INFO: Generate changelog; found commits in range: ${it.size}")
                }
                .joinToString(
                    separator = "\n", prefix = """
                        You can find the artifact on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/ch.hippmann.localizer)
                        
                        **Changelog:**
                        
                    """.trimIndent()
                ) { commit ->
                    val link = "https://github.com/chippmann/localizer/commit/${commit.id}"
                    "- [${commit.abbreviatedId}]($link) ${commit.shortMessage}"
                }

            project.buildDir.resolve("changelog.md").also {
                if (!it.parentFile.exists()) {
                    it.parentFile.mkdirs()
                }
            }.writeText(changelogString)
        }
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
