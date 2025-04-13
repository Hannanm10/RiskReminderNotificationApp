package com.example.riskreminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SecurityTipWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val tips = listOf(
        "Use strong and unique passwords for each account.",
        "Enable two-factor authentication wherever possible.",
        "Avoid clicking on suspicious links in emails or messages.",
        "Keep your apps and OS updated regularly.",
        "Review app permissions frequently to minimize risk.",
        "Install apps only from the Google Play Store or trusted sources.",
        "Regularly back up important data to a secure cloud or offline storage.",
        "Avoid using public Wi-Fi for sensitive transactions without a VPN.",
        "Use a secure screen lock method like fingerprint, PIN, or pattern.",
        "Turn off Bluetooth, location, and Wi-Fi when not in use.",
        "Disable app installations from unknown sources unless necessary.",
        "Check for app updates manually if auto-update is off.",
        "Uninstall unused apps to reduce potential attack surfaces.",
        "Beware of apps asking for excessive permissions.",
        "Enable ‘Find My Device’ to remotely locate or wipe your phone.",
        "Avoid rooting your device, as it compromises system security.",
        "Encrypt your phone if it’s not encrypted by default.",
        "Log out of apps and services you no longer use.",
        "Set app lock for sensitive apps like banking or email."
    )

    override fun doWork(): Result {
        createNotificationChannel()

        val randomTip = tips.random()

        val builder = NotificationCompat.Builder(applicationContext, "SECURITY_TIP_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Security Tip")
            .setContentText(randomTip)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(1002, builder.build())
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SECURITY_TIP_CHANNEL",
                "Security Tips Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for regular security tip notifications."
            }

            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
