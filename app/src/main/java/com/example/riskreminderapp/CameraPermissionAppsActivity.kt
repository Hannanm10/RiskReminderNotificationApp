package com.example.riskreminderapp

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme

class CameraPermissionAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiskReminderAppTheme {
                CameraPermissionAppsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CameraPermissionAppsScreen() {
        val appsWithCameraPermission = remember { getLaunchableAppsWithPermission() }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Apps with Camera Permission") })
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(appsWithCameraPermission) { app ->
                    Text(text = app.loadLabel(packageManager).toString(), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    private fun getLaunchableAppsWithPermission(): List<ApplicationInfo> {
        val packageManager = packageManager
        val launchableApps = getLaunchableApps()

        return launchableApps.filter { app ->
            packageManager.checkPermission(Manifest.permission.CAMERA, app.packageName) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        val pm = packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        return apps.mapNotNull { it.activityInfo?.applicationInfo }
    }
}
