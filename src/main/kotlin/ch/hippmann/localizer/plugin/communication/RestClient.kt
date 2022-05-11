package ch.hippmann.localizer.plugin.communication

import io.ktor.client.*

internal object RestClient {
    @Suppress("ObjectPropertyName")
    private val _client = HttpClient {

    }

    operator fun invoke() = _client
}
