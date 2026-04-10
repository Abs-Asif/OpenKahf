package com.open.kahf

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

    init {
        checkDnsStatus()
        startTimer()
    }

    fun checkDnsStatus() {
        viewModelScope.launch {
            _isDnsActive.value = dnsRepository.isDnsForFamilyActive()
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val requestTime = disableRequestTime.value
                if (requestTime > 0) {
                    val diff = (requestTime + 5 * 60 * 1000) - now
                    _remainingTime.value = if (diff > 0) diff / 1000 else 0L
                } else {
                    _remainingTime.value = 0L
                }
                delay(1000)
            }
        }
    }

    fun togglePreventChange(enabled: Boolean) {
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
}
