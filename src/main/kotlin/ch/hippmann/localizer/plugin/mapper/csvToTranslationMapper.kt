package ch.hippmann.localizer.plugin.mapper

import ch.hippmann.localizer.plugin.model.Language
import ch.hippmann.localizer.plugin.model.Translation

internal fun String.csvToTranslations(): Map<Language, List<Translation>> {
    val headerToIndexMap = provideHeaderToIndexMap(this)
    val languageHeaderToIndexMap = provideLanguageHeaderToIndexMap(headerToIndexMap)

    val translationLines = this
        .split("\r\n")
        .drop(1) // remove headers

    val languageToTranslationsMap = mutableMapOf<Language, MutableList<Translation>>()

    translationLines.flatMap { line ->
        var isInsideQuotation = false
        val commaIndices = mutableListOf<Int>()
        line.forEachIndexed { index, char ->
            if (char == ',' && !isInsideQuotation) {
                commaIndices.add(index)
            }

            if (char == '"') {
                isInsideQuotation = !isInsideQuotation
            }
        }

        var lastSplitEndIndex = 0
        val slots = mutableListOf<String>()

        commaIndices.forEach { commaIndex ->
            slots.add(line.substring(lastSplitEndIndex until commaIndex))
            lastSplitEndIndex = commaIndex + 1
        }

        slots.add(line.substring(lastSplitEndIndex))

        val keyIndex = requireNotNull(headerToIndexMap["key"]) {
            "Could not find the column index of header \"key\""
        }
        val platformIndex = requireNotNull(headerToIndexMap["platform"]) {
            "Could not find the column index of header \"platform\""
        }

        val key = slots[keyIndex]
        val platform = slots[platformIndex]

        languageHeaderToIndexMap
            .entries
            .map { (languageKey, index) ->
                languageKey to Translation(
                    key = key,
                    platform = platform,
                    translation = slots[index]
                )
            }
    }
        .filter { (_, translation) ->
            translation.key.isNotBlank() && translation.translation.isNotBlank()
        }
        .forEach { (language, translation) ->
            val translationsForLanguage = languageToTranslationsMap[language] ?: mutableListOf()
            translationsForLanguage.add(translation)
            languageToTranslationsMap[language] = translationsForLanguage
        }

    return languageToTranslationsMap
}

private fun provideLanguageHeaderToIndexMap(headerToIndexMap: Map<String, Int>) =
    headerToIndexMap
        .entries
        .filterIndexed { index, _ ->
            val platformIndex = requireNotNull(headerToIndexMap["platform"]) {
                "Could not find the column index of header \"platform\""
            }
            index > platformIndex
        }
        .associate { entry -> entry.key to entry.value }

private fun provideHeaderToIndexMap(translationCsv: String) = translationCsv
    .substringBefore("\n")
    .split(",")
    .map { it.trim() }
    .mapIndexed { index: Int, header: String -> header to index }
    .toMap()
