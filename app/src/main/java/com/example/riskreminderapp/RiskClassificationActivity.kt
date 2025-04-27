package com.example.riskreminderapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.TextFieldValue
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme
import android.app.usage.UsageStatsManager
import coil.compose.rememberAsyncImagePainter


data class AppInfo(
    val packageName: String,
    val appName: String,
    val riskLevel: String,
    val permissions: List<String>,
    val batteryUsage: Int
)

class RiskClassificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiskReminderAppTheme {
                RiskClassificationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskClassificationScreen() {
    val context = LocalContext.current
    val fullAppList = remember { getAppRiskClassification(context) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = fullAppList.filter { app ->
        app.appName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Risk Classification") })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Risk Classified Apps",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Apps") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { app ->
                        RiskLevelCard(app)
                    }
                }
            }
        }
    )
}

@Composable
fun RiskLevelCard(app: AppInfo) {
    val backgroundColor = when (app.riskLevel) {
        "High" -> Color.Red          // Solid Red
        "Medium" -> Color(0xFFFF9800) // Solid Orange
        else -> Color(0xFF4CAF50)     // Solid Green
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val appIcon = try {
        packageManager.getApplicationIcon(app.packageName)
    } catch (e: Exception) {
        null
    }

    // Map the permissions to clean names
    val importantPermissions = app.permissions.mapNotNull { permission ->
        when (permission) {
            "android.permission.CAMERA" -> "Camera"
            "android.permission.RECORD_AUDIO" -> "Microphone"
            "android.permission.ACCESS_FINE_LOCATION" -> "Location"
            else -> null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    painter = rememberAsyncImagePainter(appIcon),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Risk Level: ${app.riskLevel}", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (importantPermissions.isNotEmpty())
                        "Permissions: ${importantPermissions.joinToString()}"
                    else
                        "Permissions: None",
                    fontSize = 14.sp
                )
            }
        }
    }
}


fun getAppRiskClassification(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    val appList = mutableListOf<AppInfo>()

    for (app in apps) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            val appName = app.loadLabel(packageManager).toString()
            val packageName = app.packageName
            val permissions = try {
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val batteryUsage = getBatteryUsage(context, packageName)
            val riskLevel = classifyAppRisk(permissions, batteryUsage)

            appList.add(
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    riskLevel = riskLevel,
                    permissions = permissions,
                    batteryUsage = batteryUsage
                )
            )
        }
    }

    return appList
}

fun classifyAppRisk(permissions: List<String>?, batteryUsage: Int): String {
    val riskyPermissions = listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    var riskyPermissionCount = 0

    permissions?.let {
        for (permission in it) {
            if (riskyPermissions.contains(permission)) {
                riskyPermissionCount++
            }
        }
    }

    var riskLevel = when (riskyPermissionCount) {
        0 -> "Low"
        1 -> "Medium"
        else -> "High" // 2 or more
    }

    // Check if battery usage exceeds 1 hour (3600000 ms = 1 hour)
    if (batteryUsage > 3600000) {
        if (riskLevel == "Low") {
            riskLevel = "Medium" // Only upgrade Low to Medium, not High to Medium
        }
    }
    if (batteryUsage > 10800000) {
        riskLevel = "High" // Upgrade to High if battery usage exceeds 3 hours
    }
    return riskLevel
}


fun getBatteryUsage(context: Context, packageName: String): Int {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - (1000 * 3600 * 24)
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

    for (usageStat in stats) {
        if (usageStat.packageName == packageName) {
            return usageStat.totalTimeInForeground.toInt()
        }
    }
    return 0
}
