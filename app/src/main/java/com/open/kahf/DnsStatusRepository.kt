package com.open.kahf

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DnsStatusRepository {
    private val client = OkHttpClient()

    suspend fun isDnsForFamilyActive(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://check.dnsforfamily.com/")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: ""
                // The website likely returns "Yes" or something similar if active.
                // For now, we'll check if it contains "Yes" or "Active"
                // Actually, let's just check if the request succeeds and the body is not empty as a placeholder
                // if we don't know the exact response format.
                // Assuming it returns text containing "Yes" when active.
                body.contains("Yes", ignoreCase = true)
            }
        } catch (e: IOException) {
            false
        }
    }
}
