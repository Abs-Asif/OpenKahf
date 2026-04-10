package com.open.kahf

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val dnsRepository: DnsStatusRepository, private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _isDnsActive = MutableStateFlow(false)
    val isDnsActive: StateFlow<Boolean> = _isDnsActive.asStateFlow()

    val preventChange = settingsRepository.preventChange.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val preventUninstall = settingsRepository.preventUninstall.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val disableRequestTime = settingsRepository.disableRequestTime.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _blocklistResult = MutableStateFlow<Boolean?>(null)
    val blocklistResult: StateFlow<Boolean?> = _blocklistResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        startPeriodicCheck()
        startTimer()
    }

    fun checkDnsStatus() {
        viewModelScope.launch {
            _isDnsActive.value = dnsRepository.isDnsForFamilyActive()
        }
    }

    private fun startPeriodicCheck() {
        viewModelScope.launch {
            while (true) {
                checkDnsStatus()
                delay(60000)
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val requestTime = disableRequestTime.value
                if (requestTime > 0) {
                    val diff = (requestTime + 1 * 60 * 1000) - now
                    _remainingTime.value = if (diff > 0) diff / 1000 else 0L
                } else {
                    _remainingTime.value = 0L
                }
                delay(1000)
            }
        }
    }

    fun checkAccessibilityPermission(context: Context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        _isAccessibilityEnabled.value = enabledServices?.contains(context.packageName) == true
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
            if (!enabled) {
                if (remainingTime.value == 0L && disableRequestTime.value > 0) {
                    settingsRepository.setPreventChange(false)
                    settingsRepository.setDisableRequestTime(0L)
                } else if (disableRequestTime.value == 0L) {
                    settingsRepository.setDisableRequestTime(System.currentTimeMillis())
                }
            } else {
                settingsRepository.setPreventChange(true)
                settingsRepository.setDisableRequestTime(0L)
            }
        }
    }

    fun togglePreventUninstall(enabled: Boolean) {
        if (!isAccessibilityEnabled.value) return
        viewModelScope.launch {
            if (!enabled) {
                if (remainingTime.value == 0L && disableRequestTime.value > 0) {
                    settingsRepository.setPreventUninstall(false)
                    settingsRepository.setDisableRequestTime(0L)
                } else if (disableRequestTime.value == 0L) {
                    settingsRepository.setDisableRequestTime(System.currentTimeMillis())
                }
            } else {
                settingsRepository.setPreventUninstall(true)
                settingsRepository.setDisableRequestTime(0L)
            }
        }
    }

    fun cancelDisableRequest() {
        viewModelScope.launch {
            settingsRepository.setDisableRequestTime(0L)
        }
    }
}
