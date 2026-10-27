package com.health.knowledge.util

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("health_user_prefs", Context.MODE_PRIVATE)

    // ── FONT SIZE (Post View Page) ───────────────────────────────────────────
    private val _fontSizeSp = MutableStateFlow(prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE))
    val fontSizeSp: StateFlow<Float> = _fontSizeSp.asStateFlow()

    fun increaseFontSize() {
        val next = (_fontSizeSp.value + 2f).coerceAtMost(MAX_FONT_SIZE)
        saveFontSize(next)
    }

    fun decreaseFontSize() {
        val next = (_fontSizeSp.value - 2f).coerceAtLeast(MIN_FONT_SIZE)
        saveFontSize(next)
    }

    fun resetFontSize() {
        saveFontSize(DEFAULT_FONT_SIZE)
    }

    private fun saveFontSize(size: Float) {
        _fontSizeSp.value = size
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()
    }

    // ── HIDDEN ADMIN MODE ────────────────────────────────────────────────────
    private val _isAdminMode = MutableStateFlow(prefs.getBoolean(KEY_ADMIN_UNLOCKED, false))
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    fun verifyAndUnlockAdmin(inputPassword: String): Boolean {
        val savedPassword = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
        return if (inputPassword.trim() == savedPassword) {
            _isAdminMode.value = true
            prefs.edit().putBoolean(KEY_ADMIN_UNLOCKED, true).apply()
            true
        } else {
            false
        }
    }

    fun lockAdminMode() {
        _isAdminMode.value = false
        prefs.edit().putBoolean(KEY_ADMIN_UNLOCKED, false).apply()
    }

    fun changeAdminPassword(newPassword: String): Boolean {
        if (newPassword.trim().length < 4) return false
        prefs.edit().putString(KEY_ADMIN_PASSWORD, newPassword.trim()).apply()
        return true
    }

    fun getAdminPassword(): String {
        return prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
    }

    // ── IP / DEVICE REACTION IDENTIFIER ──────────────────────────────────────
    /**
     * Resolves the client's public IP address (cached per network session)
     * and hashes it into a safe Firestore document key so each IP can react once per post.
     * Falls back to unique device ID if offline.
     */
    suspend fun getClientIpHash(): String = withContext(Dispatchers.IO) {
        val cachedIp = prefs.getString(KEY_CACHED_IP, null)
        val lastIpFetch = prefs.getLong(KEY_IP_FETCH_TIME, 0L)
        val now = System.currentTimeMillis()

        val rawIdentifier = if (cachedIp != null && (now - lastIpFetch) < 30 * 60 * 1000L) {
            cachedIp
        } else {
            val fetchedIp = fetchPublicIp()
            if (!fetchedIp.isNullOrBlank()) {
                prefs.edit()
                    .putString(KEY_CACHED_IP, fetchedIp)
                    .putLong(KEY_IP_FETCH_TIME, now)
                    .apply()
                fetchedIp
            } else {
                cachedIp ?: getFallbackDeviceId()
            }
        }
        sha256("ip_$rawIdentifier").take(24)
    }

    private fun fetchPublicIp(): String? {
        val endpoints = listOf(
            "https://api.ipify.org",
            "https://checkip.amazonaws.com"
        )
        for (endpoint in endpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val ip = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (ip.isNotBlank() && ip.length <= 45) return ip
                }
            } catch (_: Exception) {
                // Ignore and try fallback
            }
        }
        return null
    }

    private fun getFallbackDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_FALLBACK_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }
        val finalId = if (!androidId.isNullOrBlank()) androidId else UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_FALLBACK_ID, finalId).apply()
        return finalId
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_FONT_SIZE = "article_font_size_sp"
        const val DEFAULT_FONT_SIZE = 16f
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 28f

        private const val KEY_ADMIN_UNLOCKED = "is_admin_unlocked"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        const val DEFAULT_ADMIN_PASSWORD = "admin123"

        private const val KEY_CACHED_IP = "cached_public_ip"
        private const val KEY_IP_FETCH_TIME = "cached_ip_fetch_time"
        private const val KEY_DEVICE_FALLBACK_ID = "device_fallback_id"
    }
}
