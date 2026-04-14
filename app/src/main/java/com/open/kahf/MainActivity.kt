package com.open.kahf

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dnsRepo = DnsStatusRepository()
        val settingsRepo = SettingsRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        return MainViewModel(dnsRepo, settingsRepo) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
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
fun HomeScreen(viewModel: MainViewModel) {
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

            DnsStatusCard(viewModel)

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

            BlocklistChecker(viewModel)

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
