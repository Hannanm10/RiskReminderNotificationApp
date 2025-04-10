package com.example.riskreminderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
// import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.padding
// import androidx.compose.material3.Scaffold
// import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riskreminderapp.ui.theme.RiskReminderAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiskReminderAppTheme {
                DashboardScreen()

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Risk Based Reminder App") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Stay secure with reminders", fontSize = 20.sp)

            Button(onClick = { /* TODO: Show permission alerts */ }) {
                Text("Check Permission Alerts")
            }

            Button(onClick = { /* TODO: Trigger password reminder */ }) {
                Text("Update Password Reminder")
            }

            Button(onClick = { /* TODO: Monitor camera/mic */ }) {
                Text("Monitor Camera & Microphone")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    RiskReminderAppTheme {
        DashboardScreen()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RiskReminderAppTheme {
        Greeting("Android")
    }
}