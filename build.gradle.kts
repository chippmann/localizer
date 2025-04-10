import org.ajoberstar.grgit.Grgit
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.grgit)
}

group = "ch.hippmann"
version = libs.versions.localizer.get()

gradlePlugin {
    plugins {
        create("localizer") {
            id = "ch.hippmann.localizer"
            displayName = "Generate localization files for Android and KMP"
            description =
                "Gradle plugin for generating localization files for Android projects and small Kotlin Multiplatform projects"
            implementationClass = "ch.hippmann.localizer.plugin.LocalizerGradlePlugin"
            tags.set(listOf("kotlin", "android"))
        }
        website.set("https://github.com/chippmann/localizer")
        vcsUrl.set("https://github.com/chippmann/localizer.git")
    }
    isAutomatedPublishing = true
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(libs.kotlinpoet)
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain {
        this.languageVersion = JavaLanguageVersion.of(libs.versions.toolchain.jvm.get().toInt())
        this.vendor = JvmVendorSpec.ADOPTIUM
    }
}

val grGit: Grgit = Grgit.open(mapOf("currentDir" to project.rootDir))

tasks {
    build {
        finalizedBy(publishToMavenLocal)
    }

    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.apply {
            add("-Xopt-in=kotlin.time.ExperimentalTime")
        }
    }

    @Suppress("UNUSED_VARIABLE") // used by github actions
    val generateChangelog by registering {
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

            project.layout.buildDirectory.get().asFile.resolve("changelog.md").also {
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
        @Suppress("unused")
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
