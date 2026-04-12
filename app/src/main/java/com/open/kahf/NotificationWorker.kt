package com.open.kahf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dnsRepo = DnsStatusRepository()
        val isDnsActive = dnsRepo.isDnsForFamilyActive()

        if (!isDnsActive) {
            showNotification()
        }

        return Result.success()
    }

    private fun showNotification() {
        val channelId = "dns_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "DNS Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Unprotected!")
            .setContentText("You're not protected from HARAM!!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(2, TimeUnit.HOURS)
                .addTag("dns_check_work")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "dns_check_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
