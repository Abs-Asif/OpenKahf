package com.call.logger

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.call.logger.data.AppDatabase
import com.call.logger.data.RecordingContact
import com.call.logger.worker.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleBackup()
        requestBatteryOptimizationExemption()

        setContent {
            CallLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun scheduleBackup() {
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(4, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    private fun requestBatteryOptimizationExemption() {
        val intent = Intent()
        val packageName = packageName
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val contacts by db.contactDao().getAllContacts().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newNumber by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Call Logger Configuration") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Number")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Selective Numbers to Record",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(contacts) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.phoneNumber) },
                        supportingContent = { contact.name?.let { Text(it) } },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.contactDao().deleteContact(contact)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Selective Number") },
                text = {
                    Column {
                        TextField(
                            value = newNumber,
                            onValueChange = { newNumber = it },
                            label = { Text("Phone Number") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name (Optional)") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newNumber.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                db.contactDao().insertContact(
                                    RecordingContact(newNumber, if (newName.isBlank()) null else newName)
                                )
                                withContext(Dispatchers.Main) {
                                    newNumber = ""
                                    newName = ""
                                    showDialog = false
                                }
                            }
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CallLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
