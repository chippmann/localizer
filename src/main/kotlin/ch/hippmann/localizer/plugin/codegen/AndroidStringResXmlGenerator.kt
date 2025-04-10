package ch.hippmann.localizer.plugin.codegen

import ch.hippmann.localizer.plugin.model.Language
import ch.hippmann.localizer.plugin.model.Translation
import java.io.File

internal object AndroidStringResXmlGenerator {

    fun generateTranslationFiles(
        languageToTranslationsMap: Map<Language, List<Translation>>,
        baseLanguage: Language,
        srcRoot: File
    ) {
        val translationsToProcess = languageToTranslationsMap
            .map { (language, translations) ->
                language to translations
                    .filter { translation ->
                        translation.platform != "ios"
                    }
                    .map { translation ->
                        cleanupTranslation(translation)
                    }
            }
            .toMap()

        checkForDuplicateTranslationKeys(baseLanguage, translationsToProcess)
        checkForMissingTranslations(baseLanguage, translationsToProcess)

        translationsToProcess.forEach { (language, translations) ->
            val translationFile = if (language == baseLanguage) {
                srcRoot.resolve("main/res/values/strings.xml")
            } else {
                val languageParts = language.split("-")
                // android resources expect a region to have an r prefix
                // example: de-CH becomes de-rCH
                val correctedRegion = languageParts.getOrNull(1)?.let { "r$it" }

                val normalizedLanguage = if (correctedRegion != null) {
                    languageParts
                        .toTypedArray()
                        .apply {
                            set(1, correctedRegion)
                        }
                        .joinToString("-")
                } else {
                    language
                }

                srcRoot.resolve("main/res/values-$normalizedLanguage/strings.xml")
            }.also {
                it.parentFile.mkdirs()
            }

            val translationsFileContent = buildString {
                appendLine("<resources>")

                translations
                    .forEach { translation ->
                        if (language == baseLanguage) {
                            appendLine(
                                "    <string name=\"${translation.key}\" translatable=\"${
                                    isTranslatable(
                                        baseLanguage,
                                        translation,
                                        translationsToProcess
                                    )
                                }\">${translation.translation}</string>"
                            )
                        } else {
                            appendLine("    <string name=\"${translation.key}\">${translation.translation}</string>")
                        }
                    }

                appendLine("</resources>")
            }

            translationFile.writeText(translationsFileContent)
        }
    }

    private fun cleanupTranslation(translation: Translation): Translation {
        val cleanedTranslation = translation
            .translation
            .replace("&", "&amp;")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .removePrefix("\"")
            .removeSuffix("\"")

        return translation.copy(
            translation = cleanedTranslation
        )
    }

    private fun isTranslatable(
        baseLanguage: Language,
        baseLanguageTranslationToCheck: Translation,
        languageToTranslationsMap: Map<Language, List<Translation>>
    ): Boolean {
        val nonBaseLanguages = languageToTranslationsMap
            .filter { (language, _) ->
                language != baseLanguage
            }

        return nonBaseLanguages
            .values
            .firstOrNull { translations ->
                translations
                    .any { translation ->
                        translation.key == baseLanguageTranslationToCheck.key
                    }
            } != null
    }

    private fun checkForMissingTranslations(
        baseLanguage: Language,
        languageToTranslationsMap: Map<Language, List<Translation>>,
    ) {
        val nonBaseLanguages = languageToTranslationsMap
            .filter { (language, _) ->
                language != baseLanguage
            }

        val missingTranslations = requireNotNull(languageToTranslationsMap[baseLanguage])
            .filter { translation ->
                isTranslatable(baseLanguage, translation, nonBaseLanguages)
            }
            .map { baseLanguageTranslation ->
                val foundTranslations = nonBaseLanguages
                    .mapValues { (_, translations) ->
                        translations.filter { translation ->
                            translation.key == baseLanguageTranslation.key
                        }
                    }

                baseLanguageTranslation to foundTranslations
                    .filter { (_, translations) ->
                        translations.isEmpty()
                    }
                    .keys
            }
            .filter { (_, languagesWithMissingTranslation) ->
                languagesWithMissingTranslation.isNotEmpty()
            }

        if (missingTranslations.isNotEmpty()) {
            val errorText = buildString {
                appendLine("There are missing translations:")

                missingTranslations.forEach { (translation, languages) ->
                    appendLine("${translation.key}: [${languages.joinToString()}]")
                }
            }

            throw IllegalStateException(errorText)
        }
    }

    private fun checkForDuplicateTranslationKeys(
        baseLanguage: Language,
        languageToTranslationsMap: Map<Language, List<Translation>>
    ) {
        val translationsWithDuplicateKey = requireNotNull(languageToTranslationsMap[baseLanguage])
            .groupBy { translation ->
                translation.key
            }
            .filter { (_, translationsWithSameKey) ->
                translationsWithSameKey.size > 1
            }

        if (translationsWithDuplicateKey.isNotEmpty()) {
            val errorText = buildString {
                appendLine("There are multiple translations with the same key:")

                translationsWithDuplicateKey.forEach { (key, translations) ->
                    appendLine("${key}: Translations in base language -> [${translations.joinToString { it.translation }}]")
                }
            }

            throw IllegalStateException(errorText)
        }
    }
}
