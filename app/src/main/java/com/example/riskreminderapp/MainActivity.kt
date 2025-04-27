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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    private lateinit var cameraGranted: MutableState<Boolean>
    private lateinit var micGranted: MutableState<Boolean>
    private lateinit var locationGranted: MutableState<Boolean>
    private lateinit var usageAccessGranted: MutableState<Boolean>

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
            cameraGranted = remember { mutableStateOf(hasCameraPermission()) }
            micGranted = remember { mutableStateOf(hasMicPermission()) }
            locationGranted = remember { mutableStateOf(hasLocationPermission()) }
            var showUsageAccessDialog by remember { mutableStateOf(false) }
            usageAccessGranted = remember { mutableStateOf(hasUsageStatsPermission()) }

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



//                if (hasUsageStatsPermission()) {
//                    val intent = Intent(context, AppUsageMonitorService::class.java)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        startForegroundService(intent)
//                    } else {
//                        startService(intent)
//                    }
//                } else {
//                    showUsageAccessDialog = true
//                }

            }

            // Dynamic re-launching and stopping of services on permission change
            LaunchedEffect(cameraGranted.value) {
                if (cameraGranted.value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(
                            Intent(
                                context,
                                CameraMonitorService::class.java
                            )
                        )
                    } else {
                        startService(Intent(context, CameraMonitorService::class.java))
                    }
                } else {
                    stopService(Intent(context, CameraMonitorService::class.java))
                }
            }

            schedulePasswordReminder()
            scheduleSecurityTipsReminder()
            if (hasUsageStatsPermission()) {
                scheduleDailyBatteryAlert()
            }
            // Start monitoring service based on available permissions
            startFeatureMonitorService()

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

            if (showUsageAccessDialog) {
                AskUsageAccessDialog(
                    onConfirm = {
                        showUsageAccessDialog = false
                        openUsageAccessSettings(this@MainActivity)
                    },
                    onDismiss = {
                        showUsageAccessDialog = false
                    }
                )
            }

            RiskReminderAppTheme {
                DashboardScreen(
                    onRequestCamera = {
                        checkAndRequestCameraPermission {
                            showSettingsDialog = true
                            cameraGranted.value = hasCameraPermission()
                        }
                        cameraGranted.value = hasCameraPermission()
                    },
                    onRequestMic = {
                        checkAndRequestMicPermission {
                            showSettingsDialog = true
                            micGranted.value = hasMicPermission()
                        }
                        micGranted.value = hasMicPermission()
                    },
                    onRequestLocation = {
                        checkAndRequestLocationPermissions {
                            showSettingsDialog = true
                            locationGranted.value = hasLocationPermission()
                        }
                        locationGranted.value = hasLocationPermission()
                    },
                    isCameraEnabled = cameraGranted.value,
                    isMicEnabled = micGranted.value,
                    isLocationEnabled = locationGranted.value,
                    isUsageAccessGranted = usageAccessGranted.value,
                    onViewCameraApps = {
                        if (cameraGranted.value) {
                            startActivity(Intent(context, CameraPermissionAppsActivity::class.java))
                        } else {
                            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onViewMicApps = {
                        if (micGranted.value) {
                            startActivity(Intent(context, MicPermissionAppsActivity::class.java))
                        } else {
                            Toast.makeText(context, "Mic permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onViewLocationApps = {
                        if (locationGranted.value) {
                            startActivity(Intent(context, LocationPermissionAppsActivity::class.java))
                        } else {
                            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRequestUsageAccess = {
                        showUsageAccessDialog = true
                        usageAccessGranted.value = hasUsageStatsPermission()
                    },
                    onViewBatterySummary = {
                        if (hasUsageStatsPermission()) {
                            startActivity(Intent(context, BatterySummaryActivity::class.java))
                        } else {
                            Toast.makeText(context, "Usage stats permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onViewRiskClassification = {
                        if (hasUsageStatsPermission()) {
                            startActivity(Intent(context, RiskClassificationActivity::class.java))
                        } else {
                            Toast.makeText(context, "Usage stats permission not granted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission states
        refreshPermissions()
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

    private fun scheduleDailyBatteryAlert() {
        val workRequest = PeriodicWorkRequestBuilder<TopBatteryDrainerWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun checkAndRequestCameraPermission(onPermanentlyDenied: () -> Unit) {
        handlePermissions(listOf(Manifest.permission.CAMERA), onPermanentlyDenied)
    }

    private fun checkAndRequestMicPermission(onPermanentlyDenied: () -> Unit) {
        handlePermissions(listOf(Manifest.permission.RECORD_AUDIO), onPermanentlyDenied)
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

    private fun startFeatureMonitorService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (hasCameraPermission()) {
                startForegroundService(Intent(this, CameraMonitorService::class.java))
            }
//            if (hasMicPermission()) {
//                startForegroundService(Intent(this, MicMonitorService::class.java))
//            }
//            if (hasLocationPermission()) {
//                startForegroundService(Intent(this, LocationMonitorService::class.java))
//            }
        } else {
            if (hasCameraPermission()) {
                startService(Intent(this, CameraMonitorService::class.java))
            }
//            if (hasMicPermission()) {
//                startService(Intent(this, MicMonitorService::class.java))
//            }
//            if (hasLocationPermission()) {
//                startService(Intent(this, LocationMonitorService::class.java))
//            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun refreshPermissions() {
        // Refresh permission states
        val cameraNow = hasCameraPermission()
        val micNow = hasMicPermission()
        val locationNow = hasLocationPermission()
        val usageAccessNow = hasUsageStatsPermission()

        // Update the UI
        if (this::cameraGranted.isInitialized) {
            cameraGranted.value = cameraNow
        }
        if (this::micGranted.isInitialized) {
            micGranted.value = micNow
        }
        if (this::locationGranted.isInitialized) {
            locationGranted.value = locationNow
        }
        if (this::usageAccessGranted.isInitialized) {
            usageAccessGranted.value = usageAccessNow
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun openUsageAccessSettings(context: android.content.Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onRequestCamera: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestLocation: () -> Unit,
    isCameraEnabled: Boolean,
    isMicEnabled: Boolean,
    isLocationEnabled: Boolean,
    isUsageAccessGranted: Boolean,
    onViewCameraApps: () -> Unit,
    onViewMicApps: () -> Unit,
    onViewLocationApps: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onViewBatterySummary: () -> Unit,
    onViewRiskClassification: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Risk Reminder Security App") })
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Stay Secure!", fontSize = 40.sp)

            if (!isCameraEnabled) {
                Button(onClick = onRequestCamera) {
                    Text("Enable Camera Monitoring")
                }
            }

            if (!isMicEnabled) {
                Button(onClick = onRequestMic) {
                    Text("Enable Microphone Monitoring")
                }
            }

            if (!isLocationEnabled) {
                Button(onClick = onRequestLocation) {
                    Text("Enable Location Monitoring")
                }
            }

            if (!isUsageAccessGranted) {
                Button(onClick = onRequestUsageAccess) {
                    Text("Enable Usage Access Monitoring")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Divider()

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = onViewCameraApps) {
                Text("View Apps with Camera Access")
            }
            Button(onClick = onViewMicApps) {
                Text("View Apps with Mic Access")
            }
            Button(onClick = onViewLocationApps) {
                Text("View Apps with Location Access")
            }
            Button(onClick = onViewBatterySummary) {
                Text("View Battery Summary")
            }
            Button(onClick = onViewRiskClassification) {
                Text("View Apps Risk Classification")
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
            Text("You've permanently denied some permission. Would you like to open settings to allow it?")
        }
    )
}

@Composable
fun AskUsageAccessDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Usage Access Required") },
        text = {
            Text("This app needs permission to access your usage stats for various features to work. Would you like to enable it in settings?")
        }
    )
}


@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    RiskReminderAppTheme {
        DashboardScreen({}, {}, {}, false, false, false, false, {}, {}, {}, {}, {}, {})
    }
}
