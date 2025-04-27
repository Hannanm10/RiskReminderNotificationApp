package com.example.riskreminderapp

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme
import java.util.*

class BatterySummaryActivity : ComponentActivity() {

    private lateinit var usageStatsManager: UsageStatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        setContent {
            RiskReminderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BatteryUsageScreen(getBatteryUsageList())
                }
            }
        }
    }

    private fun getBatteryUsageList(): List<Triple<String, String, String>> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

        val usageStatsList =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

        return usageStatsList
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .map {
                val appName = getAppName(it.packageName)
                val timeUsedMillis = it.totalTimeInForeground
                val formattedTime = formatDuration(timeUsedMillis)
                val riskLevel = calculateRiskLevel(timeUsedMillis)
                Triple(appName, formattedTime, riskLevel)
            }
    }

    private fun calculateRiskLevel(timeMillis: Long): String {
        val hours = timeMillis / 1000 / 60 / 60

        return when {
            hours >= 3 -> "High Risk"
            hours >= 1 -> "Medium Risk"
            else -> "Low Risk"
        }
    }


    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 1000 / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours}h ${remainingMinutes}m"
    }
}

@Composable
fun BatteryUsageScreen(batteryUsageList: List<Triple<String, String, String>>) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Battery Usage in Last 24 Hours", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn {
            items(batteryUsageList) { (appName, usageTime, riskLevel) ->
                val appIcon = remember { getAppIcon(context, context.packageName) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (riskLevel) {
                            "High Risk" -> Color.Red
                            "Medium Risk" -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Display the app icon
                        appIcon?.let {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = "$appName Icon",
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = 12.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Usage: $usageTime",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Risk: $riskLevel",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = when (riskLevel) {
                                    "High Risk" -> Color.Red
                                    "Medium Risk" -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Function to get the app icon
fun getAppIcon(context: Context, packageName: String): Drawable? {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationIcon(appInfo)
    } catch (e: Exception) {
        null
    }
}

