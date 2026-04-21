package com.call.logger.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class BackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Supabase configuration - to be provided by user later
    private val SUPABASE_URL = "https://your-project.supabase.co"
    private val SUPABASE_KEY = "your-api-key"
    private val STORAGE_BUCKET = "call-recordings"

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        val recordingsDir = File(applicationContext.filesDir, "recordings")
        if (!recordingsDir.exists() || !recordingsDir.isDirectory) {
            return Result.success()
        }

        val files = recordingsDir.listFiles() ?: return Result.success()

        var allSuccessful = true
        for (file in files) {
            if (file.isFile && !file.name.endsWith(".uploaded")) {
                val success = uploadToSupabase(file)
                if (success) {
                    // Mark file as uploaded by renaming it or using a separate log
                    val uploadedFile = File(file.absolutePath + ".uploaded")
                    file.renameTo(uploadedFile)
                } else {
                    allSuccessful = false
                }
            }
        }

        return if (allSuccessful) Result.success() else Result.retry()
    }

    private fun uploadToSupabase(file: File): Boolean {
        if (SUPABASE_KEY == "your-api-key") {
            Log.w("BackupWorker", "Supabase API key not provided. Skipping upload for ${file.name}")
            return false
        }

        val requestBody = file.asRequestBody("application/octet-stream".toMediaType())
        val url = "$SUPABASE_URL/storage/v1/object/$STORAGE_BUCKET/${file.name}"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/octet-stream")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("BackupWorker", "Successfully uploaded ${file.name}")
                    true
                } else {
                    Log.e("BackupWorker", "Failed to upload ${file.name}: ${response.code} ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("BackupWorker", "Error uploading ${file.name}", e)
            false
        }
    }
}
