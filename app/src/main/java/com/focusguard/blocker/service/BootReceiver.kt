package com.focusguard.blocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Accessibility services are automatically restarted by the system after boot.
        // Hook left here for future use (e.g. re-post a status notification).
    }
}
