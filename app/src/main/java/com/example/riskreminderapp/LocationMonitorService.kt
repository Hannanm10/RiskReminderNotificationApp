package com.example.riskreminderapp

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class LocationMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "location_monitor_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Monitor Active")
            .setContentText("Monitoring location usage...")
            .setSmallIcon(R.drawable.baseline_add_location_alt_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(3, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

//    private fun createNotification(content: String): Notification {
//        return NotificationCompat.Builder(this, "monitoring_channel")
//            .setContentTitle("Risk Reminder App")
//            .setContentText(content)
//            // .setSmallIcon(R.drawable.ic_security) // Replace with your icon
//            .setOngoing(true)
//            .build()
//    }
}
