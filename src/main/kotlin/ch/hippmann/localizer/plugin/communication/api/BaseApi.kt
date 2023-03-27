package ch.hippmann.localizer.plugin.communication.api

import ch.hippmann.localizer.plugin.logging.err
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration

internal open class BaseApi {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = Duration.seconds(20).inWholeMilliseconds
            connectTimeoutMillis = Duration.seconds(20).inWholeMilliseconds
            socketTimeoutMillis = Duration.seconds(20).inWholeMilliseconds
        }
    }

    suspend fun <T> get(
        url: String,
        builder: HttpRequestBuilder.() -> Unit,
        converter: (String) -> T
    ): Result<T> = withContext(Dispatchers.Default) {
        try {
            val response = client.get(url) {
                builder(this)
            }

            if (response.status.isSuccess()) {
                val bodyString = response.body<String>()
                val convertedBody = try {
                    converter(bodyString)
                } catch (e: Throwable) {
                    err(e) {
                        "Could not deserialize: $bodyString in call to $url"
                    }
                    null
                }

                if (convertedBody == null) {
                    Result.failure(IllegalStateException("Could not deserialize $bodyString from url $url"))
                } else {
                    Result.success(convertedBody)
                }
            } else {
                throw IllegalStateException("Non successful response should already been caught at this stage. But got unsuccessful response: $response")
            }
        } catch (e: ResponseException) {
            Result.failure(e)
        }
    }
}
