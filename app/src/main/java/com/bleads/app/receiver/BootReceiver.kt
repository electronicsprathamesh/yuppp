package com.bleads.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bleads.app.service.BLEScanningService
import com.bleads.app.util.PreferencesHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Check if user is logged in
            val preferencesHelper = PreferencesHelper(context)
            if (preferencesHelper.isLoggedIn()) {
                // Restart the BLE scanning service
                val serviceIntent = Intent(context, BLEScanningService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
