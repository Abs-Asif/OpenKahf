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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dnsRepo = DnsStatusRepository()
        val settingsRepo = SettingsRepository(applicationContext)
        val prayerRepo = PrayerTimesRepository()
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(dnsRepo, settingsRepo, prayerRepo) as T
                }
            }
        )[MainViewModel::class.java]

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                OpenKahfApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAccessibilityPermission(this)
        viewModel.checkUsagePermission(this)
        viewModel.checkDnsStatus()
        viewModel.fetchAppUsage(this)
    }
}

@Composable
fun OpenKahfApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("home") }

    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF4A2C7E)
            ) {
                NavigationBarItem(
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentScreen == "prayer",
                    onClick = { currentScreen = "prayer" },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = "Prayer", modifier = Modifier.size(24.dp)) },
                    label = { Text("Prayer", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentScreen == "usage",
                    onClick = { currentScreen = "usage" },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Usage") },
                    label = { Text("Usage", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentScreen == "settings",
                    onClick = { currentScreen = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "home" -> HomeScreen(viewModel)
                    "prayer" -> PrayerScreen(viewModel)
                    "usage" -> UsageScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                }
            }
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
                        color = Color(0xFF1A1C1E)
                    )
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
        }
    }
}

@Composable
fun PrayerScreen(viewModel: MainViewModel) {
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val currentWaqtName by viewModel.currentWaqtName.collectAsState()
    val waqtRemainingTime by viewModel.waqtRemainingTime.collectAsState()
    val waqtProgress by viewModel.waqtProgress.collectAsState()

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
            Text(
                text = "Prayer Times",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (prayerTimes != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2C7E))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentWaqtName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = waqtRemainingTime,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { 1f - waqtProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrayerTimesList(viewModel, prayerTimes!!)
            } else {
                CircularProgressIndicator(color = Color(0xFF4A2C7E))
            }
        }
    }
}

@Composable
fun UsageScreen(viewModel: MainViewModel) {
    val isUsagePermissionGranted by viewModel.isUsagePermissionGranted.collectAsState()
    val appUsageData by viewModel.appUsageData.collectAsState()
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
            Text(
                text = "App Usage",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isUsagePermissionGranted) {
                PermissionItem(
                    title = "Usage Stats Permission",
                    description = "Required to show how much time you spend on each app.",
                    action = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    actionLabel = "Grant",
                    isEnabled = true
                )
            } else {
                UsageGraph(appUsageData)
                Spacer(modifier = Modifier.height(24.dp))
                AppUsageList(appUsageData)
            }
        }
    }
}

@Composable
fun UsageGraph(usageData: List<AppUsageInfo>) {
    if (usageData.isEmpty()) return

    val totalDailyUsage = LongArray(7) { 0L }
    usageData.forEach { info ->
        info.dailyUsage.forEachIndexed { index, time ->
            if (index < 7) totalDailyUsage[index] += time
        }
    }

    val maxUsage = totalDailyUsage.maxOrNull() ?: 1L

    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Usage Last 7 Days", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = size.width / 7
                val maxBarHeight = size.height - 20.dp.toPx()

                totalDailyUsage.forEachIndexed { index, usage ->
                    val barHeight = (usage.toFloat() / maxUsage.toFloat()) * maxBarHeight
                    val x = index * spacing + spacing / 4
                    val y = size.height - barHeight

                    drawRect(
                        color = Color(0xFF4A2C7E),
                        topLeft = Offset(x, y),
                        size = Size(spacing / 2, barHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val preventChange by viewModel.preventChange.collectAsState()
    val preventUninstall by viewModel.preventUninstall.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
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
                text = "Settings",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1C1E)
            )

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

@Composable
fun PrayerTimesList(viewModel: MainViewModel, times: Map<String, String>) {
    val waqts = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return 0
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        for (i in waqts.indices) {
            val waqt = waqts[i]
            val time = times[waqt] ?: "--:--"
            val nextWaqt = if (i == waqts.size - 1) waqts[0] else waqts[i + 1]
            val nextTime = times[nextWaqt] ?: "--:--"

            val minutes = timeToMinutes(time)
            val nextMinutes = timeToMinutes(nextTime)

            var durationMinutes = nextMinutes - minutes
            if (durationMinutes < 0) durationMinutes += 24 * 60

            val duration = String.format("%dh %dm", durationMinutes / 60, durationMinutes % 60)

            PrayerTimeItem(viewModel, waqt, time, duration)
        }
    }
}

@Composable
fun PrayerTimeItem(viewModel: MainViewModel, name: String, time: String, duration: String) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.schedulePrayerNotification(context, name, time)
            Toast.makeText(context, "Notification scheduled for $name", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = viewModel.formatTo12Hour(time), fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = duration, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.schedulePrayerNotification(context, name, time)
                                Toast.makeText(context, "Notification scheduled for $name", Toast.LENGTH_SHORT).show()
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.schedulePrayerNotification(context, name, time)
                            Toast.makeText(context, "Notification scheduled for $name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notify", tint = Color(0xFF4A2C7E), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AppUsageList(usageData: List<AppUsageInfo>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        usageData.forEach { info ->
            AppUsageItem(info)
        }
    }
}

@Composable
fun AppUsageItem(info: AppUsageInfo) {
    val hours = info.totalTime / (1000 * 60 * 60)
    val minutes = (info.totalTime / (1000 * 60)) % 60

    val networkUsageMB = info.networkUsage / (1024 * 1024)
    val networkUsageGB = info.networkUsage.toDouble() / (1024 * 1024 * 1024)
    val networkString = if (networkUsageGB >= 1.0) {
        String.format("%.2f GB", networkUsageGB)
    } else {
        "$networkUsageMB MB"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (info.icon != null) {
                Image(
                    bitmap = info.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Data: $networkString",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = String.format("%dh %dm", hours, minutes),
                fontSize = 14.sp,
                color = Color(0xFF4A2C7E),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
