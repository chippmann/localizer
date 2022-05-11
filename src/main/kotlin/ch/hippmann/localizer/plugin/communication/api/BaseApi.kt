package ch.hippmann.localizer.plugin.communication.api

import ch.hippmann.localizer.plugin.communication.RestClient
import ch.hippmann.localizer.plugin.logging.err
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal open class BaseApi {
    suspend fun <T> get(
        url: String,
        builder: HttpRequestBuilder.() -> Unit,
        converter: (String) -> T
    ): Result<T> = withContext(Dispatchers.Default) {
        try {
            val response = RestClient().get(url) {
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
