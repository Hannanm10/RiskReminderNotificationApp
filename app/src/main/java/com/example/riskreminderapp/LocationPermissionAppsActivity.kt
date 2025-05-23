package com.example.riskreminderapp

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme

class LocationPermissionAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiskReminderAppTheme {
                LocationPermissionAppsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LocationPermissionAppsScreen() {
        val context = LocalContext.current
        val appsWithLocationPermission = remember { getLaunchableAppsWithPermission(context) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Apps with Location Permission") })
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->

            if (appsWithLocationPermission.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No apps with location permission.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(appsWithLocationPermission) { app ->
                        LocationPermissionAppCard(app)
                    }
                }
            }
        }
    }

    @Composable
    fun LocationPermissionAppCard(app: ApplicationInfo) {
        val context = LocalContext.current
        val packageManager = context.packageManager
        val appIcon = try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(appIcon),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.loadLabel(packageManager).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Has location access",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    private fun getLaunchableAppsWithPermission(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        val launchableApps = getLaunchableApps()

        return launchableApps.filter { app ->
            packageManager.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, app.packageName) == PackageManager.PERMISSION_GRANTED
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
