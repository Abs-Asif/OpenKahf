package com.open.kahf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val dnsRepo = remember { DnsStatusRepository() }
            val settingsRepo = remember { SettingsRepository(context) }
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(dnsRepo, settingsRepo) as T
                    }
                }
            )

            OpenKahfApp(viewModel)
        }
    }
}

@Composable
fun OpenKahfApp(viewModel: MainViewModel) {
    val isDnsActive by viewModel.isDnsActive.collectAsState()
    val preventChange by viewModel.preventChange.collectAsState()
    val preventUninstall by viewModel.preventUninstall.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "OpenKahf",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDnsActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isDnsActive) "DNS for Family: ACTIVE" else "DNS for Family: INACTIVE",
                        fontWeight = FontWeight.Bold,
                        color = if (isDnsActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Button(onClick = { viewModel.checkDnsStatus() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Refresh Status")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Guide: Set Private DNS to dns-dot.dnsforfamily.com in your phone settings.",
                fontSize = 14.sp
            )
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) // Closest common settings page
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Open DNS Settings")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Permissions & Security",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            ListItem(
                headlineContent = { Text("Accessibility Permission") },
                supportingContent = { Text("Required for 'Prevent' features. Highly Recommended.") },
                trailingContent = {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("Grant")
                    }
                }
            )

            Divider()

            PreventionToggle(
                label = "Prevent Change",
                description = "Stops user from changing Private DNS settings.",
                checked = preventChange,
                onCheckedChange = { viewModel.togglePreventChange(it) },
                remainingTime = remainingTime
            )

            PreventionToggle(
                label = "Prevent Uninstall",
                description = "Stops user from uninstalling OpenKahf.",
                checked = preventUninstall,
                onCheckedChange = { viewModel.togglePreventUninstall(it) },
                remainingTime = remainingTime
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "API Documentation for Developers",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "URL: https://dnsforfamily.com/api/checkHost\n" +
                        "Parameters: 'hostnames' (string|array)\n" +
                        "Response: JSON { success, result: { hostname: bool } }",
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PreventionToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    remainingTime: Long
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Column {
                Text(description)
                if (checked && remainingTime > 0) {
                    Text("Unlock in: ${remainingTime / 60}m ${remainingTime % 60}s", color = Color.Red)
                }
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
