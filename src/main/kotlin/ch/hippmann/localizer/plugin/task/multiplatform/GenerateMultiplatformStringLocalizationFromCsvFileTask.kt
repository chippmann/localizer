package ch.hippmann.localizer.plugin.task.multiplatform

import ch.hippmann.localizer.plugin.codegen.MultiplatformTranslationCodeGenerator
import ch.hippmann.localizer.plugin.logging.info
import ch.hippmann.localizer.plugin.mapper.csvToTranslations
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class GenerateMultiplatformStringLocalizationFromCsvFileTask : DefaultTask() {
    @InputFile
    val csvFile: RegularFileProperty = project.objects.fileProperty()

    @OutputDirectory
    val srcRoot: RegularFileProperty = project.objects.fileProperty()

    @Input
    val translationFilePackagePath: Property<String> = project.objects.property(String::class.java)

    @Input
    val baseLanguage: Property<String> = project.objects.property(String::class.java)

    @TaskAction
    fun generateStringLocalizationForAndroid() {
        val translationCsvAsString = csvFile.get().asFile.readText()

        info { "Converting csv to [language to translations] map" }
        val languageToTranslationsMap = translationCsvAsString.csvToTranslations()

        info { "Generating translation file" }
        MultiplatformTranslationCodeGenerator.generateTranslationFile(
            languageToTranslationsMap = languageToTranslationsMap,
            baseLanguage = baseLanguage.getOrElse("en"),
            srcRoot = srcRoot.get().asFile,
            packagePath = translationFilePackagePath.get()
        )
    }
}
