package com.open.kahf

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

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
