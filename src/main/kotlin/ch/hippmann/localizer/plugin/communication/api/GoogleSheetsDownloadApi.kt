package ch.hippmann.localizer.plugin.communication.api

import io.ktor.client.request.*

internal object GoogleSheetsDownloadApi: BaseApi() {
    suspend fun getSheetAsCsv(sheetId: String, sheetIndex: Int): Result<String> {
        return get(
            url = "https://docs.google.com/spreadsheets/d/$sheetId/export",
            builder = {
                parameter("format", "csv")
                parameter("id", sheetId)
                parameter("gid", sheetIndex)
            },
            converter = { it }
        )
    }
}
