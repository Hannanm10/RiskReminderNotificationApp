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


class PasswordReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {


    override fun doWork(): Result {
        // Display a notification
        createNotificationChannel()

        val builder = NotificationCompat.Builder(applicationContext, "PASSWORD_REMINDER_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Password Reminder")
            .setContentText("It's time to update your passwords for better security.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Ensure that permission to post notifications is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(applicationContext).notify(1001, builder.build())
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Password Reminder Notifications"
            val descriptionText = "Channel for sending password reminder notifications."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PASSWORD_REMINDER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
