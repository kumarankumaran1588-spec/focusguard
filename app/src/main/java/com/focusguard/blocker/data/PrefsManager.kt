package com.focusguard.blocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Central store for the password (as a salted hash) and the set of blocked packages.
 * Everything is written through Android's EncryptedSharedPreferences, so the data
 * at rest is encrypted with a key held in the hardware-backed keystore.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "focusguard_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---------- Password ----------

    fun isPasswordSet(): Boolean = prefs.contains(KEY_PW_HASH)

    fun setPassword(plain: String) {
        prefs.edit().putString(KEY_PW_HASH, hash(plain)).apply()
    }

    fun checkPassword(plain: String): Boolean =
        prefs.getString(KEY_PW_HASH, null) == hash(plain)

    // ---------- Blocked apps ----------

    fun getBlockedApps(): Set<String> =
        prefs.getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()

    fun setBlockedApps(pkgs: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED, pkgs).apply()
    }

    // ---------- Temporary unlocks ----------
    // When the user enters the password on the block screen we let them use that
    // app for a short grace period instead of nagging them every second.

    fun grantTempAccess(pkg: String, minutes: Int = 5) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong(TEMP_PREFIX + pkg, until).apply()
    }

    fun isTempAllowed(pkg: String): Boolean =
        System.currentTimeMillis() < prefs.getLong(TEMP_PREFIX + pkg, 0L)

    companion object {
        private const val KEY_PW_HASH = "pw_hash"
        private const val KEY_BLOCKED = "blocked_apps"
        private const val TEMP_PREFIX = "temp_"

        // A fixed app-level salt. EncryptedSharedPreferences already protects the
        // value; the salt just avoids storing a bare, rainbow-tableable digest.
        private const val SALT = "fg_v1_5c3a9d"

        private fun hash(plain: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((SALT + plain).toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
