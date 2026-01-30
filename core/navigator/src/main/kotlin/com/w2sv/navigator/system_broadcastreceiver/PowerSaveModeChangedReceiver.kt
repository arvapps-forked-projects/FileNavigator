package com.w2sv.navigator.system_broadcastreceiver

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.w2sv.androidutils.service.systemService
import com.w2sv.navigator.FileNavigator
import slimber.log.i

class PowerSaveModeChangedReceiver : DynamicBroadcastReceiver(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {

    override fun onReceiveMatchingIntent(context: Context, intent: Intent) {
        val powerManager = context.systemService<PowerManager>()
        if (powerManager.isPowerSaveMode) {
            FileNavigator.stop(context)
            i { "Stopped navigator due to power save mode" }
        }
    }
}
