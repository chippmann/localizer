package ch.hippmann.localizer.plugin.task.common

import ch.hippmann.localizer.plugin.communication.api.GoogleSheetsDownloadApi
import ch.hippmann.localizer.plugin.logging.warn
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class DownloadGoogleSheetsAsCsvTask : DefaultTask() {
    @Input
    val sheetId: Property<String> = project.objects.property(String::class.java)

    @Input
    val sheetIndex: Property<Int> = project.objects.property(Int::class.java)

    @OutputFile
    val outFile: RegularFileProperty = project.objects.fileProperty()

    @Input
    val throwIfDownloadFailed: Property<Boolean> = project.objects.property(Boolean::class.java)

    @TaskAction
    fun downloadGoogleSheetsAsCsv() {
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

        outFile.get().asFile.writeText(result.getOrThrow())
    }
}
