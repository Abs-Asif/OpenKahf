package com.open.kahf

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PrayerTimesRepository {
    private val client = OkHttpClient()
    private val apiKey = "J8NkVS5dXdS0IzzbXDI0mL0sAkJHo0aj44jdm0Kh4fTdg699"

    suspend fun getPrayerTimes(lat: Double, lon: Double): Map<String, String>? = withContext(Dispatchers.IO) {
        val url = "https://islamicapi.com/api/v1/prayer-time/?lat=$lat&lon=$lon&method=4&school=1&api_key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optString("status") == "success") {
                    val data = json.getJSONObject("data")
                    val times = data.getJSONObject("times")
                    val result = mutableMapOf<String, String>()
                    val keys = times.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        result[key] = times.getString(key)
                    }
                    return@withContext result
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
