package com.focusguard.blocker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focusguard.blocker.data.PrefsManager
import com.focusguard.blocker.databinding.ActivityBlockBinding

/**
 * The wall the user hits when they open a blocked app. Two ways out:
 *   - "Close" sends them back home (the intended, healthy path)
 *   - entering the password grants a short temporary access window
 */
class BlockOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockBinding
    private lateinit var prefs: PrefsManager
    private var blockedPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        blockedPkg = intent.getStringExtra(EXTRA_PACKAGE)

        binding.btnClose.setOnClickListener { goHome() }

        binding.btnUnlock.setOnClickListener {
            val pw = binding.password.text.toString()
            val pkg = blockedPkg
            if (pkg != null && prefs.checkPassword(pw)) {
                prefs.grantTempAccess(pkg, minutes = 5)
                Toast.makeText(this, "Unlocked for 5 minutes", Toast.LENGTH_SHORT).show()
                finish() // return to the app they were opening
            } else {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goHome() {
        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    // Back button just goes home — never silently back into the blocked app.
    override fun onBackPressed() { goHome() }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
    }
}
