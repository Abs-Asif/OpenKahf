package com.call.logger.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val CHANNEL_ID = "CallRecordingServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("PHONE_NUMBER") ?: "Unknown"
        val action = intent?.action

        if (action == "START_RECORDING") {
            startRecording(phoneNumber)
        } else if (action == "STOP_RECORDING") {
            stopRecording()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startRecording(phoneNumber: String) {
        if (isRecording) return

        val notification = createNotification(phoneNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val fileName = "Call_${phoneNumber}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.amr"
        val outputDir = File(filesDir, "recordings")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Log.d("RecordingService", "Recording started: ${outputFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("RecordingService", "MediaRecorder prepare() failed", e)
                stopSelf()
            } catch (e: IllegalStateException) {
                Log.e("RecordingService", "MediaRecorder start() failed", e)
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("RecordingService", "Stop recording failed", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            Log.d("RecordingService", "Recording stopped")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Call Recording Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(phoneNumber: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Call")
            .setContentText("Recording call with $phoneNumber")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
