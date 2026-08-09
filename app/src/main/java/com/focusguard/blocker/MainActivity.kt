package com.focusguard.blocker

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.blocker.admin.BlockerDeviceAdminReceiver
import com.focusguard.blocker.data.PrefsManager
import com.focusguard.blocker.databinding.ActivityMainBinding
import com.focusguard.blocker.service.AppBlockerService
import com.focusguard.blocker.ui.AppSelectionActivity
import com.focusguard.blocker.ui.PasswordActivity

/**
 * The control panel. Walks the user through the three things that must be enabled
 * for the app to work: (1) a password, (2) the accessibility service, (3) device
 * admin for uninstall protection. Also the entry point to pick blocked apps.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        // Force a password on first run before anything else can be configured.
        if (!prefs.isPasswordSet()) {
            startActivity(Intent(this, PasswordActivity::class.java).apply {
                putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_SET)
            })
        }

        binding.btnSetPassword.setOnClickListener {
            val mode = if (prefs.isPasswordSet()) PasswordActivity.MODE_CHANGE
                       else PasswordActivity.MODE_SET
            startActivity(Intent(this, PasswordActivity::class.java)
                .putExtra(PasswordActivity.EXTRA_MODE, mode))
        }

        binding.btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnDeviceAdmin.setOnClickListener {
            if (isAdminActive()) {
                // Removing protection requires the password (handled in PasswordActivity).
                startActivity(Intent(this, PasswordActivity::class.java)
                    .putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_DISABLE_ADMIN))
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        BlockerDeviceAdminReceiver.getComponent(this@MainActivity))
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.device_admin_explanation))
                }
                startActivity(intent)
            }
        }

        binding.btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        binding.statusPassword.text = statusLine("Password", prefs.isPasswordSet())
        binding.statusApps.text =
            "Blocked apps: ${prefs.getBlockedApps().size} selected"
        binding.statusAccessibility.text =
            statusLine("Accessibility service", isAccessibilityEnabled())
        binding.statusOverlay.text =
            statusLine("Display over other apps", Settings.canDrawOverlays(this))
        binding.statusDeviceAdmin.text =
            statusLine("Uninstall protection", isAdminActive())
    }

    private fun statusLine(label: String, ok: Boolean) =
        if (ok) "\u2705 $label: ON" else "\u274C $label: OFF"

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(BlockerDeviceAdminReceiver.getComponent(this))
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name == AppBlockerService::class.java.name
        }
    }
}
