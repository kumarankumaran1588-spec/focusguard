package com.focusguard.blocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focusguard.blocker.data.PrefsManager
import com.focusguard.blocker.ui.BlockOverlayActivity
import com.focusguard.blocker.ui.PasswordActivity

/**
 * The heart of the app. Android calls onAccessibilityEvent whenever the foreground
 * window changes. We check which app came to the front and, if it is on the block
 * list, throw up the full-screen block activity. We also watch for the user trying
 * to reach this app's "App info" / uninstall / device-admin screens and demand the
 * password first (tamper protection).
 */
class AppBlockerService : AccessibilityService() {

    private lateinit var prefs: PrefsManager

    // Debounce so we don't launch the block screen dozens of times per second.
    private var lastHandledPkg: String? = null
    private var lastHandledAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PrefsManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Never act on ourselves.
        if (pkg == packageName) return

        // ---- Tamper protection ----
        // If the user opens the system Settings or the package installer, check
        // whether the screen is about *this* app (app info / uninstall / admin).
        if (pkg == "com.android.settings" ||
            pkg.contains("packageinstaller") ||
            pkg.contains("permissioncontroller")
        ) {
            if (screenMentionsUs()) {
                promptPasswordForTamper()
                return
            }
        }

        // ---- App blocking ----
        val blocked = prefs.getBlockedApps()
        if (blocked.contains(pkg) && !prefs.isTempAllowed(pkg)) {
            val now = System.currentTimeMillis()
            // Avoid re-triggering repeatedly for the same package in a short window.
            if (pkg == lastHandledPkg && now - lastHandledAt < 800) return
            lastHandledPkg = pkg
            lastHandledAt = now
            showBlockScreen(pkg)
        }
    }

    /**
     * Best-effort check that the current settings screen refers to our app. Searches
     * the on-screen node tree for our app name or the word "uninstall". This is
     * intentionally conservative; see README for the known limits of this approach.
     */
    private fun screenMentionsUs(): Boolean {
        val root = rootInActiveWindow ?: return false
        val needles = buildList {
            add("FocusGuard")
            val labelRes = applicationInfo.labelRes
            if (labelRes != 0) add(getString(labelRes))
        }
        for (needle in needles) {
            if (needle.isNotBlank() &&
                root.findAccessibilityNodeInfosByText(needle).isNotEmpty()
            ) {
                return true
            }
        }
        return false
    }

    private fun promptPasswordForTamper() {
        // Send the user home first, then demand the password. If they get it wrong
        // they simply never reach the uninstall/deactivate button.
        performGlobalAction(GLOBAL_ACTION_HOME)
        val intent = Intent(this, PasswordActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_TAMPER)
        }
        startActivity(intent)
    }

    private fun showBlockScreen(blockedPkg: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE, blockedPkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* no-op */ }
}
