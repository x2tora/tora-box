package com.x2tora.torabox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.x2tora.torabox.worker.ShipmentSyncScheduler

class LiveUpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_REFRESH_NOW) {
            ShipmentSyncScheduler.triggerImmediateSync(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_NOW = "com.x2tora.torabox.ACTION_LIVE_UPDATE_REFRESH"
    }
}
