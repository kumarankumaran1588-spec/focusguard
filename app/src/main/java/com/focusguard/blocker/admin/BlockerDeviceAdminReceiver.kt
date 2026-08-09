package com.focusguard.blocker.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

/**
 * Being an *active* device admin is what actually stops Android from letting the
 * app be uninstalled directly. To remove the app the user must first deactivate
 * this admin — and our accessibility service watches for that screen and asks for
 * the password before it can happen.
 */
class BlockerDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: android.content.Intent): CharSequence {
        // Shown on the system "deactivate admin" confirmation screen.
        return "FocusGuard protection will be turned off and blocked apps will become accessible."
    }

    companion object {
        fun getComponent(context: Context): ComponentName =
            ComponentName(context, BlockerDeviceAdminReceiver::class.java)
    }
}
