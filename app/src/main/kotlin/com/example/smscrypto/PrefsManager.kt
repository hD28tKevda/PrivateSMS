package com.example.smscrypto

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREFS_NAME = "sms_crypto_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMyPrivateKey(context: Context): String? {
        return getPrefs(context).getString("my_private_key", null)
    }

    fun setMyPrivateKey(context: Context, key: String?) {
        getPrefs(context).edit().putString("my_private_key", key).apply()
    }

    fun getMyPublicKey(context: Context): String? {
        return getPrefs(context).getString("my_public_key", null)
    }

    fun setMyPublicKey(context: Context, key: String?) {
        getPrefs(context).edit().putString("my_public_key", key).apply()
    }

    fun getContactPublicKey(context: Context, phone: String): String? {
        return getPrefs(context).getString("pubkey_$phone", null)
    }

    fun setContactPublicKey(context: Context, phone: String, key: String?) {
        getPrefs(context).edit().putString("pubkey_$phone", key).apply()
    }

    fun isContactMuted(context: Context, phone: String): Boolean {
        return getPrefs(context).getBoolean("muted_$phone", false)
    }

    fun setContactMuted(context: Context, phone: String, muted: Boolean) {
        getPrefs(context).edit().putBoolean("muted_$phone", muted).apply()
    }

    fun isContactBlocked(context: Context, phone: String): Boolean {
        return getPrefs(context).getBoolean("blocked_$phone", false)
    }

    fun setContactBlocked(context: Context, phone: String, blocked: Boolean) {
        getPrefs(context).edit().putBoolean("blocked_$phone", blocked).apply()
    }

    // По умолчанию шифрование выключено
    fun isEncryptionEnabled(context: Context, phone: String): Boolean {
        return getPrefs(context).getBoolean("encrypt_$phone", false)
    }

    fun setEncryptionEnabled(context: Context, phone: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("encrypt_$phone", enabled).apply()
    }

    fun isChatRead(context: Context, phone: String): Boolean {
        return getPrefs(context).getBoolean("read_$phone", true)
    }

    fun setChatRead(context: Context, phone: String, read: Boolean) {
        getPrefs(context).edit().putBoolean("read_$phone", read).apply()
    }
}