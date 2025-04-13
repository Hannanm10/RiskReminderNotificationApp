package com.example.riskreminderapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private var onPermissionsDeniedPermanently: (() -> Unit)? = null

    private var isNotificationPermissionPermanentlyDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val permanentlyDenied = permissions.filterValues { !it }.keys.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }

            if (permanentlyDenied) {
                onPermissionsDeniedPermanently?.invoke()
            } else {
                val allGranted = permissions.values.all { it }
                Toast.makeText(
                    this,
                    if (allGranted) "All permissions granted" else "Some permissions denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (!shouldShow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isNotificationPermissionPermanentlyDenied = true
                }
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            var showSettingsDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                // Ask for Notification permission if not already granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!granted) {
                        val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (shouldShow) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // if permanently denied, show settings dialog
                            showSettingsDialog = true
                        }
                    }
                }

                schedulePasswordReminder()
                scheduleSecurityTipsReminder()
            }

            if (showSettingsDialog) {
                AskSettingsDialog(
                    onConfirm = {
                        showSettingsDialog = false
                        openAppSettings(context)
                    },
                    onDismiss = {
                        showSettingsDialog = false
                    }
                )
            }

            RiskReminderAppTheme {
                DashboardScreen(
                    onRequestCameraMic = {
                        checkAndRequestCameraAndMicPermissions {
                            showSettingsDialog = true
                        }
                    },
                    onRequestLocation = {
                        checkAndRequestLocationPermissions {
                            showSettingsDialog = true
                        }
                    },
                    onPasswordReminderClicked = {
                        Toast.makeText(
                            context,
                            "Password reminders are automatically scheduled.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun schedulePasswordReminder() {
        val workRequest = PeriodicWorkRequestBuilder<PasswordReminderWorker>(
            7, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun scheduleSecurityTipsReminder() {
        val tipRequest = PeriodicWorkRequestBuilder<SecurityTipWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueue(tipRequest)
    }

    private fun checkAndRequestCameraAndMicPermissions(onPermanentlyDenied: () -> Unit) {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        handlePermissions(permissions, onPermanentlyDenied)
    }

    private fun checkAndRequestLocationPermissions(onPermanentlyDenied: () -> Unit) {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        handlePermissions(permissions, onPermanentlyDenied)
    }

    private fun handlePermissions(permissions: List<String>, onPermanentlyDenied: () -> Unit) {
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            Toast.makeText(this, "Permissions already granted", Toast.LENGTH_SHORT).show()
        } else {
            onPermissionsDeniedPermanently = onPermanentlyDenied
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun openAppSettings(context: android.content.Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onRequestCameraMic: () -> Unit,
    onRequestLocation: () -> Unit,
    onPasswordReminderClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Risk Based Reminder App") })
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Stay secure with reminders", fontSize = 20.sp)

            Button(onClick = onRequestCameraMic) {
                Text("Enable Camera & Mic Monitoring")
            }

            Button(onClick = onRequestLocation) {
                Text("Enable Location Monitoring")
            }

            Button(onClick = onPasswordReminderClicked) {
                Text("Update Password Reminder")
            }
        }
    }
}

@Composable
fun AskSettingsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Permission Required") },
        text = {
            Text("You've permanently denied notification permission. Would you like to open settings to allow it?")
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    RiskReminderAppTheme {
        DashboardScreen({}, {}, {})
    }
}
