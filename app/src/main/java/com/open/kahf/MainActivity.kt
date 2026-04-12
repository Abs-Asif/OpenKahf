package com.open.kahf

import android.content.ClipboardManager
import android.content.ClipData
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings
import android.os.PowerManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dnsRepo = DnsStatusRepository()
        val settingsRepo = SettingsRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(dnsRepo, settingsRepo) as T
                }
            }
        )[MainViewModel::class.java]

        viewModel.registerNetworkCallback(this)
        NotificationWorker.schedule(this)

        setContent {
            MaterialTheme {
                OpenKahfApp(viewModel, onExit = { finish() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterNetworkCallback(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAccessibilityPermission(this)
        viewModel.checkDnsStatus(this)
    }
}

@Composable
fun OpenKahfApp(viewModel: MainViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val isPinSet by viewModel.isPinSet.collectAsState()
    var isAuthenticated by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(viewModel.areAllPermissionsGranted(context)) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Re-check permissions when returning to app
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionsGranted = viewModel.areAllPermissionsGranted(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit") },
            text = { Text("Are you sure you want to close the app?") },
            confirmButton = {
                Button(onClick = onExit) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }

    BackHandler {
        showExitDialog = true
    }

    if (!permissionsGranted) {
        PermissionsOnboardingScreen(viewModel) {
            permissionsGranted = true
        }
    } else if (!isPinSet) {
        PinSetupScreen(viewModel)
    } else if (!isAuthenticated) {
        PinEntryScreen(viewModel) {
            isAuthenticated = true
        }
    } else {
        HomeScreen(viewModel)
    }
}

@Composable
fun PermissionsOnboardingScreen(viewModel: MainViewModel, onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions Required", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "To provide full protection, OpenKahf needs the following permissions:",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            PermissionOnboardingItem(
                title = "Notifications",
                description = "To alert you when you are not protected.",
                isGranted = hasNotificationPermission,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            PermissionOnboardingItem(
                title = "Accessibility",
                description = "To prevent unauthorized changes to DNS settings.",
                isGranted = isAccessibilityEnabled,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            PermissionOnboardingItem(
                title = "Unrestricted Usage",
                description = "To prevent the app from being stopped in the background.",
                isGranted = isIgnoringBatteryOptimizations,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (viewModel.areAllPermissionsGranted(context)) {
                        onAllGranted()
                    } else {
                        Toast.makeText(context, "Please grant all permissions to continue", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
fun PermissionOnboardingItem(title: String, description: String, isGranted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isGranted) Color(0xFFF0F9F0) else Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, fontSize = 12.sp, color = Color.Gray)
            }
            if (isGranted) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.checkbox_on_background),
                    contentDescription = "Granted",
                    tint = Color(0xFF4CAF50)
                )
            } else {
                Button(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PinSetupScreen(viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Set 6-digit PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it },
                label = { Text("New PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6) confirmPin = it },
                label = { Text("Confirm PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (pin.length == 6 && pin == confirmPin) {
                        viewModel.setPin(pin)
                    }
                },
                enabled = pin.length == 6 && pin == confirmPin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set PIN")
            }
        }
    }
}

@Composable
fun PinEntryScreen(viewModel: MainViewModel, onAuthenticated: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6) {
                        pin = it
                        if (it.length == 6) {
                            scope.launch {
                                if (viewModel.verifyPin(it)) {
                                    onAuthenticated()
                                } else {
                                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                                    pin = ""
                                }
                            }
                        }
                    }
                },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val isDnsActive by viewModel.isDnsActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val blocklistResult by viewModel.blocklistResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val context = LocalContext.current

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
                modifier = Modifier.fillMaxWidth().animateContentSize(),
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
                        color = Color(0xFF1A1C1E),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.checkDnsStatus(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF4A2C7E))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("DNS URL", "https://check.dnsforfamily.com/")
                    clipboard.setPrimaryClip(clip)

                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C7E))
            ) {
                Text("Open Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
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
                        .padding(top = 12.dp)
                        .animateContentSize(),
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

            SettingsSection(viewModel)
        }
    }
}

@Composable
fun SettingsSection(viewModel: MainViewModel) {
    val preventChange by viewModel.preventChange.collectAsState()
    val preventUninstall by viewModel.preventUninstall.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

    val context = LocalContext.current
    var showChangePinDialog by remember { mutableStateOf(false) }

    if (showChangePinDialog) {
        ChangePinDialog(viewModel, onDismiss = { showChangePinDialog = false })
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            PermissionItem(
                title = "Notification Permission",
                description = "Required to show DNS status alerts.",
                action = {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                actionLabel = "Grant",
                isEnabled = !hasNotificationPermission
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))
        }

        PreventionToggleItem(
            label = "Prevent Change",
            description = "Stops users from changing the Private DNS settings.",
            checked = preventChange,
            onCheckedChange = {
                viewModel.togglePreventChange(it)
            },
            enabled = isAccessibilityEnabled
        )

        PreventionToggleItem(
            label = "Prevent Uninstall",
            description = "Stops users from uninstalling the OpenKahf app.",
            checked = preventUninstall,
            onCheckedChange = {
                viewModel.togglePreventUninstall(it)
            },
            enabled = isAccessibilityEnabled
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))

        Button(
            onClick = { showChangePinDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C7E))
        ) {
            Text("Change PIN")
        }

        Spacer(modifier = Modifier.height(32.dp))

        val packageInfo = remember {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }
        }

        Text(
            text = "Version: ${packageInfo?.versionName ?: "Unknown"} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo?.longVersionCode else packageInfo?.versionCode})",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun ChangePinDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6) oldPin = it },
                    label = { Text("Old PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 6) confirmNewPin = it },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.verifyPin(oldPin)) {
                            if (newPin.length == 6 && newPin == confirmNewPin) {
                                viewModel.setPin(newPin)
                                onDismiss()
                                Toast.makeText(context, "PIN changed successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "New PINs do not match or are not 6 digits", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Incorrect old PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
