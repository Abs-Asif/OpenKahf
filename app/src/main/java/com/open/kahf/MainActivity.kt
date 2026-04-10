package com.open.kahf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val dnsRepo = remember { DnsStatusRepository() }
            val settingsRepo = remember { SettingsRepository(context) }
            viewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(dnsRepo, settingsRepo) as T
                    }
                }
            )

            OpenKahfApp(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAccessibilityPermission(this)
        viewModel.checkDnsStatus()
    }
}

@Composable
fun OpenKahfApp(viewModel: MainViewModel) {
    val isDnsActive by viewModel.isDnsActive.collectAsState()
    val preventChange by viewModel.preventChange.collectAsState()
    val preventUninstall by viewModel.preventUninstall.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val blocklistResult by viewModel.blocklistResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val disableRequestTime by viewModel.disableRequestTime.collectAsState()

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var pendingToggleType by remember { mutableStateOf("") }

    if (showDialog) {
        if (remainingTime > 0) {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss */ },
                title = { Text("Please Wait") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You'll have to wait for a minute viewing the popup and wait.")
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { (60L - remainingTime).toFloat() / 60f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.cancelDisableRequest()
                            showDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        } else if (disableRequestTime > 0) {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss */ },
                title = { Text("Ready") },
                text = { Text("The wait time is over. You can now turn off the setting.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pendingToggleType == "change") {
                                viewModel.togglePreventChange(false)
                            } else if (pendingToggleType == "uninstall") {
                                viewModel.togglePreventUninstall(false)
                            }
                            showDialog = false
                            pendingToggleType = ""
                        }
                    ) {
                        Text("Turn Off Now")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.cancelDisableRequest()
                            showDialog = false
                            pendingToggleType = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "OpenKahf",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDnsActive) Color(0xFFF0F9F0) else Color(0xFFFFF5F5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isDnsActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DNS Status: ${if (isDnsActive) "Active" else "Inactive"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1C1E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent("android.settings.NETWORK_OPERATOR_SETTINGS")
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C7E))
            ) {
                Text("Open DNS Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Blocklist Checker",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search URL to check...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F3F4),
                    unfocusedContainerColor = Color(0xFFF1F3F4),
                    disabledContainerColor = Color(0xFFF1F3F4),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchHost() })
            )

            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (blocklistResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (blocklistResult == true) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    )
                ) {
                    Text(
                        text = if (blocklistResult == true) "Blocked" else "Accessible",
                        modifier = Modifier.padding(16.dp),
                        color = if (blocklistResult == true) Color.Red else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissions & Security",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionItem(
                title = "Accessibility Permission",
                description = "Required to prevent users from changing settings or uninstalling the app.",
                action = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                actionLabel = "Grant",
                isEnabled = !isAccessibilityEnabled
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))

            PreventionToggleItem(
                label = "Prevent Change",
                description = "Stops users from changing the Private DNS settings.",
                checked = preventChange,
                onCheckedChange = {
                    if (it) {
                        viewModel.togglePreventChange(true)
                    } else {
                        pendingToggleType = "change"
                        viewModel.togglePreventChange(false)
                        showDialog = true
                    }
                },
                enabled = isAccessibilityEnabled
            )

            PreventionToggleItem(
                label = "Prevent Uninstall",
                description = "Stops users from uninstalling the OpenKahf app.",
                checked = preventUninstall,
                onCheckedChange = {
                    if (it) {
                        viewModel.togglePreventUninstall(true)
                    } else {
                        pendingToggleType = "uninstall"
                        viewModel.togglePreventUninstall(false)
                        showDialog = true
                    }
                },
                enabled = isAccessibilityEnabled
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = buildAnnotatedString {
                    append("Created and Maintained by ")
                    withStyle(style = SpanStyle(color = Color(0xFF4A2C7E), fontWeight = FontWeight.Bold)) {
                        append("Md. Abdullah Bari Asif")
                    }
                },
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun PermissionItem(title: String, description: String, action: () -> Unit, actionLabel: String, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
        }
        if (isEnabled) {
            Button(
                onClick = action,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C7E))
            ) {
                Text(actionLabel)
            }
        } else {
            Text("Granted", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PreventionToggleItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
