package com.open.kahf

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OpenKahfAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        serviceScope.launch {
            val preventChange = settingsRepository.preventChange.first()
            val preventUninstall = settingsRepository.preventUninstall.first()

            if (preventChange) {
                handlePreventChange(event)
            }

            if (preventUninstall) {
                handlePreventUninstall(event)
            }
        }
    }

    private fun handlePreventChange(event: AccessibilityEvent) {
        // Detecting Private DNS settings.
        // This is tricky as it varies by OEM, but usually contains "Private DNS"
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("settings", ignoreCase = true)) {
            val rootNode = rootInActiveWindow ?: return
            if (findText(rootNode, "Private DNS")) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun handlePreventUninstall(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("packageinstaller", ignoreCase = true) ||
            packageName.contains("settings", ignoreCase = true)) {

            val rootNode = rootInActiveWindow ?: return
            // Look for uninstallation dialog or text related to OpenKahf
            if (findText(rootNode, "OpenKahf") &&
                (findText(rootNode, "uninstall") || findText(rootNode, "delete"))) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun findText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findText(child, text)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
