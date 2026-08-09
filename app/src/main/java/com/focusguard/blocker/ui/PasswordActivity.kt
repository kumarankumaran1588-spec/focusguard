package com.focusguard.blocker.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.blocker.admin.BlockerDeviceAdminReceiver
import com.focusguard.blocker.data.PrefsManager
import com.focusguard.blocker.databinding.ActivityPasswordBinding

/**
 * One screen, several jobs, selected via EXTRA_MODE:
 *   SET           first-time password creation
 *   CHANGE        change password (requires old one)
 *   TAMPER        block screen for settings/uninstall attempts
 *   DISABLE_ADMIN verify password, then remove device-admin so the app can be uninstalled
 */
class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private lateinit var prefs: PrefsManager
    private var mode: String = MODE_SET

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SET

        when (mode) {
            MODE_SET -> setupCreate(confirmNeeded = true, title = "Create a password")
            MODE_CHANGE -> setupCreate(confirmNeeded = true, title = "Change password", verifyOld = true)
            MODE_TAMPER -> setupVerify("Enter password to change settings")
            MODE_DISABLE_ADMIN -> setupVerify("Enter password to remove protection")
        }
    }

    private fun setupCreate(confirmNeeded: Boolean, title: String, verifyOld: Boolean = false) {
        binding.title.text = title
        binding.oldPassword.visibility = if (verifyOld) android.view.View.VISIBLE else android.view.View.GONE
        binding.confirmPassword.visibility = if (confirmNeeded) android.view.View.VISIBLE else android.view.View.GONE

        binding.btnSubmit.setOnClickListener {
            val pw = binding.password.text.toString()
            val confirm = binding.confirmPassword.text.toString()

            if (verifyOld && !prefs.checkPassword(binding.oldPassword.text.toString())) {
                toast("Current password is wrong"); return@setOnClickListener
            }
            if (pw.length < 4) { toast("Use at least 4 characters"); return@setOnClickListener }
            if (confirmNeeded && pw != confirm) { toast("Passwords don't match"); return@setOnClickListener }

            prefs.setPassword(pw)
            toast("Password saved")
            finish()
        }
    }

    private fun setupVerify(title: String) {
        binding.title.text = title
        binding.oldPassword.visibility = android.view.View.GONE
        binding.confirmPassword.visibility = android.view.View.GONE

        binding.btnSubmit.setOnClickListener {
            val pw = binding.password.text.toString()
            if (!prefs.checkPassword(pw)) { toast("Wrong password"); return@setOnClickListener }

            if (mode == MODE_DISABLE_ADMIN) {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                dpm.removeActiveAdmin(BlockerDeviceAdminReceiver.getComponent(this))
                toast("Protection removed. You can now uninstall the app.")
            }
            finish()
        }

        // In tamper mode the back button shouldn't be an escape hatch.
        if (mode == MODE_TAMPER) {
            onBackPressedDispatcher.addCallback(this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() { finish() }
                })
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_SET = "set"
        const val MODE_CHANGE = "change"
        const val MODE_TAMPER = "tamper"
        const val MODE_DISABLE_ADMIN = "disable_admin"
    }
}
