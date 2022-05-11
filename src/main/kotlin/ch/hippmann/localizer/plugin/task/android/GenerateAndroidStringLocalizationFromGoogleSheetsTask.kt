package ch.hippmann.localizer.plugin.task.android

import ch.hippmann.localizer.plugin.codegen.AndroidStringResXmlGenerator
import ch.hippmann.localizer.plugin.communication.api.GoogleSheetsDownloadApi
import ch.hippmann.localizer.plugin.logging.info
import ch.hippmann.localizer.plugin.logging.warn
import ch.hippmann.localizer.plugin.mapper.csvToTranslations
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class GenerateAndroidStringLocalizationFromGoogleSheetsTask : DefaultTask() {
    @Input
    val sheetId: Property<String> = project.objects.property(String::class.java)

    @Input
    val sheetIndex: Property<Int> = project.objects.property(Int::class.java)

    @OutputDirectory
    val srcRoot: RegularFileProperty = project.objects.fileProperty()

    @Input
    val throwIfDownloadFailed: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    val baseLanguage: Property<String> = project.objects.property(String::class.java)

    init {
        // always execute this task
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generateStringLocalizationForAndroid() {
        val result = runBlocking {
            GoogleSheetsDownloadApi.getSheetAsCsv(
                sheetId = sheetId.get(),
                sheetIndex = sheetIndex.get()
            )
        }

        if (result.isFailure) {
            if (throwIfDownloadFailed.getOrElse(true)) {
                throw IllegalStateException("Could not download csv. Error was: ${result.exceptionOrNull()?.message}")
            } else {
                warn { "Could not download csv. Will not generate any translation files. Will not fail build as \"throwIfFailed\" is set to false. Error was: ${result.exceptionOrNull()?.message}" }
            }
            return
        }

        val translationCsvAsString = result.getOrThrow()

        info { "Converting csv to [language to translations] map" }
        val languageToTranslationsMap = translationCsvAsString.csvToTranslations()

        info { "Generating translation files" }
        AndroidStringResXmlGenerator.generateTranslationFiles(
            languageToTranslationsMap = languageToTranslationsMap,
            baseLanguage = baseLanguage.getOrElse("en"),
            srcRoot = srcRoot.get().asFile
        )
    }
}
