package com.example.riskreminderapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.app.usage.UsageStatsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AppOpsManager
import android.os.Process

class CameraMonitorService : Service() {

    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var availabilityCallback: CameraManager.AvailabilityCallback
    private var lastUsedApp: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        setupCameraAvailabilityCallback()
    }

    private fun setupCameraAvailabilityCallback() {
        availabilityCallback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                super.onCameraAvailable(cameraId)
                showCameraUsageNotification("Camera is not being used right now.")
            }

            override fun onCameraUnavailable(cameraId: String) {
                super.onCameraUnavailable(cameraId)

                if (hasUsageStatsPermission()) {
                    val appName = getForegroundApp()
                    if (appName != null) {
                        showCameraUsageNotification("Camera is being used by: $appName")
                    } else {
                        showCameraUsageNotification("Camera is being used")
                    }
                } else {
                    showCameraUsageNotification("Camera is being used")
                }
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 8000,
            currentTime
        )

        if (!usageStatsList.isNullOrEmpty()) {
            val sortedStats = usageStatsList.sortedByDescending { it.lastTimeUsed }
            return sortedStats.firstOrNull()?.packageName
        }
        return null
    }

    private fun showCameraUsageNotification(message: String) {
        if (lastUsedApp != message) {
            lastUsedApp = message

            val notification = NotificationCompat.Builder(this, "cameraChannel")
                .setContentTitle("Camera Monitoring Active")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cameraChannel",
                "Camera Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when camera is being used"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, "cameraChannel")
            .setContentTitle("Camera Monitor Active")
            .setContentText("Monitoring camera usage...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }


    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            cameraManager.registerAvailabilityCallback(availabilityCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
}
