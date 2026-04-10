package com.open.kahf

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.RemoteException
import android.provider.Settings
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainViewModel(
    private val dnsRepository: DnsStatusRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

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

    private val _prayerTimes = MutableStateFlow<Map<String, String>?>(null)
    val prayerTimes: StateFlow<Map<String, String>?> = _prayerTimes.asStateFlow()

    private val _isUsagePermissionGranted = MutableStateFlow(false)
    val isUsagePermissionGranted: StateFlow<Boolean> = _isUsagePermissionGranted.asStateFlow()

    private val _appUsageData = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appUsageData: StateFlow<List<AppUsageInfo>> = _appUsageData.asStateFlow()

    init {
        startPeriodicCheck()
        startTimer()
        fetchPrayerTimes()
    }

    fun fetchPrayerTimes() {
        viewModelScope.launch {
            // Dhaka coordinates
            val lat = 23.8103
            val lon = 90.4125
            _prayerTimes.value = prayerTimesRepository.getPrayerTimes(lat, lon)
        }
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
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        _isAccessibilityEnabled.value = enabledServices?.contains(context.packageName) == true
    }

    fun checkUsagePermission(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 60, now)
        _isUsagePermissionGranted.value = stats != null && stats.isNotEmpty()
    }

    fun schedulePrayerNotification(context: Context, name: String, time: String) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val now = Calendar.getInstance()
        val prayerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (prayerTime.before(now)) {
            prayerTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = prayerTime.timeInMillis - now.timeInMillis

        val data = Data.Builder()
            .putString("prayer_name", name)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("prayer_$name")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun fetchAppUsage(context: Context) {
        if (!_isUsagePermissionGranted.value) return

        viewModelScope.launch {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

            val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

            val usageList = usageStats.values
                .filter { it.totalTimeInForeground > 0 }
                .map { stat ->
                    val networkUsage = getNetworkUsageForPackage(context, networkStatsManager, stat.packageName, startTime, endTime)
                    AppUsageInfo(stat.packageName, stat.totalTimeInForeground, networkUsage)
                }
                .sortedByDescending { it.totalTime }
                .take(10)

            _appUsageData.value = usageList
        }
    }

    private fun getNetworkUsageForPackage(
        context: Context,
        networkStatsManager: NetworkStatsManager,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        return try {
            val packageManager = context.packageManager
            val uid = packageManager.getPackageUid(packageName, 0)

            val wifiStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime,
                uid
            )

            val mobileStats = networkStatsManager.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime,
                uid
            )

            var totalBytes = 0L
            val bucket = NetworkStats.Bucket()

            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                totalBytes += bucket.rxBytes + bucket.txBytes
            }
            wifiStats.close()

            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                totalBytes += bucket.rxBytes + bucket.txBytes
            }
            mobileStats.close()

            totalBytes
        } catch (e: Exception) {
            0L
        }
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

data class AppUsageInfo(val packageName: String, val totalTime: Long, val networkUsage: Long)
