package com.example.riskreminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TopBatteryDrainerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val usageStatsManager =
            applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val topApp = usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .maxByOrNull { it.totalTimeInForeground }

        topApp?.let {
            val appName = getAppName(applicationContext, it.packageName)
            val formattedTime = formatDuration(it.totalTimeInForeground)

            sendBatteryNotification(appName, formattedTime)
        }

        return Result.success()
    }

    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun formatDuration(timeMillis: Long): String {
        val totalSeconds = timeMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    private fun sendBatteryNotification(appName: String, usageTime: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "battery_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("⚡ High Usage Detected")
            .setContentText("$appName used for $usageTime today")
            .setSmallIcon(R.drawable.baseline_battery_alert_24) // Replace with your app's icon
            .build()

        notificationManager.notify(2024, notification)
    }
}
