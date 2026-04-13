package com.open.kahf

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class DnsStatusRepository(private val client: OkHttpClient = NetworkModule.okHttpClient) {

    suspend fun isDnsForFamilyActive(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://verify.dnsforfamily.com/")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: return@withContext false
                val json = JSONObject(body)
                json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkHost(hostname: String): Boolean? = withContext(Dispatchers.IO) {
        val encodedHostname = try {
            URLEncoder.encode(hostname, "UTF-8")
        } catch (e: Exception) {
            hostname
        }
        val url = "https://dnsforfamily.com/api/checkHost?hostnames[]=$encodedHostname"
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optBoolean("success")) {
                    val result = json.optJSONObject("result")
                    return@withContext result?.optBoolean(hostname)
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
