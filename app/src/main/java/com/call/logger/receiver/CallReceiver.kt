package com.call.logger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.call.logger.data.AppDatabase
import com.call.logger.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("CallLoggerPrefs", Context.MODE_PRIVATE)

        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            if (outgoingNumber != null) {
                prefs.edit().putString("LAST_NUMBER", outgoingNumber).apply()
                Log.d("CallReceiver", "Outgoing call detected: $outgoingNumber")
            }
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d("CallReceiver", "State: $state, IncomingNumber: $incomingNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (incomingNumber != null) {
                    prefs.edit().putString("LAST_NUMBER", incomingNumber).apply()
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val phoneNumber = prefs.getString("LAST_NUMBER", null)
                if (phoneNumber != null) {
                    checkAndStartRecording(context, phoneNumber)
                } else {
                    Log.w("CallReceiver", "OFFHOOK but no phone number found in prefs")
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                stopRecording(context)
                prefs.edit().remove("LAST_NUMBER").apply()
            }
        }
    }

    private fun checkAndStartRecording(context: Context, phoneNumber: String) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val contact = db.contactDao().getContactByNumber(phoneNumber)
            if (contact != null) {
                Log.d("CallReceiver", "Number $phoneNumber is in selective list. Starting recording.")
                val serviceIntent = Intent(context, RecordingService::class.java).apply {
                    action = "START_RECORDING"
                    putExtra("PHONE_NUMBER", phoneNumber)
                }
                context.startForegroundService(serviceIntent)
            } else {
                Log.d("CallReceiver", "Number $phoneNumber is NOT in selective list.")
            }
        }
    }

    private fun stopRecording(context: Context) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = "STOP_RECORDING"
        }
        context.stopService(serviceIntent)
    }
}
