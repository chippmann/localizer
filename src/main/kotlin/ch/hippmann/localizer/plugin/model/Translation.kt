package ch.hippmann.localizer.plugin.model

internal data class Translation(
    val key: TranslationKey,
    val platform: Platform,
    val translation: String
)

internal typealias TranslationKey = String
internal typealias Language = String
internal typealias Platform = String
