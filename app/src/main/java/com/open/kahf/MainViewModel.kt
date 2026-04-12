package com.open.kahf

import android.content.Context
import android.os.Build
import android.net.ConnectivityManager
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.Manifest
import android.content.pm.PackageManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat

class MainViewModel(
    private val dnsRepository: DnsStatusRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isDnsActive = MutableStateFlow(false)
    val isDnsActive: StateFlow<Boolean> = _isDnsActive.asStateFlow()

    val preventChange = settingsRepository.preventChange.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val preventUninstall = settingsRepository.preventUninstall.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _blocklistResult = MutableStateFlow<Boolean?>(null)
    val blocklistResult: StateFlow<Boolean?> = _blocklistResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val isPinSet = settingsRepository.isPinSet.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        startPeriodicCheck()
    }

    fun checkDnsStatus(context: Context? = null) {
        viewModelScope.launch {
            val apiCheck = dnsRepository.isDnsForFamilyActive()

            val systemCheck = if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                val dnsServer = linkProperties?.privateDnsServerName
                dnsServer != null && (dnsServer.contains("dnsforfamily.com") || dnsServer.contains("kahfguard.com"))
            } else {
                false
            }

            _isDnsActive.value = apiCheck || systemCheck
        }
    }

    fun registerNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                checkDnsStatus(context)
            }

            override fun onLost(network: Network) {
                checkDnsStatus(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                checkDnsStatus(context)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
                checkDnsStatus(context)
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    fun unregisterNetworkCallback(context: Context) {
        networkCallback?.let {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun startPeriodicCheck() {
        viewModelScope.launch {
            while (true) {
                // If context is null, it only does API check
                checkDnsStatus()
                delay(60000)
            }
        }
    }

    fun checkAccessibilityPermission(context: Context) {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        _isAccessibilityEnabled.value = enabledServices?.contains(context.packageName) == true
    }

    fun areAllPermissionsGranted(context: Context): Boolean {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val accessibilityGranted = _isAccessibilityEnabled.value

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptimizationsIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        return notificationGranted && accessibilityGranted && batteryOptimizationsIgnored
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _blocklistResult.value = null
        }
    }

    fun searchHost() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            _blocklistResult.value = dnsRepository.checkHost(query)
            _isSearching.value = false
        }
    }

    fun togglePreventChange(enabled: Boolean) {
        if (!isAccessibilityEnabled.value) return
        viewModelScope.launch {
            settingsRepository.setPreventChange(enabled)
        }
    }

    fun togglePreventUninstall(enabled: Boolean) {
        if (!isAccessibilityEnabled.value) return
        viewModelScope.launch {
            settingsRepository.setPreventUninstall(enabled)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val savedPin = settingsRepository.pin.first()
        return savedPin == pin
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setPin(pin)
        }
    }
}
