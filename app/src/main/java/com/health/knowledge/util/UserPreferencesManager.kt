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

    // ── READING THEME MODE ("system" | "light" | "sepia" | "dark") ───────────
    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun cycleThemeMode(): String {
        val next = when (_themeMode.value) {
            THEME_SYSTEM -> THEME_LIGHT
            THEME_LIGHT -> THEME_SEPIA
            THEME_SEPIA -> THEME_DARK
            else -> THEME_SYSTEM
        }
        setThemeMode(next)
        return next
    }

    // ── SORT ORDER ("newest" | "popular" | "oldest") — Default: NEWEST FIRST ─
    private val _sortOrder = MutableStateFlow(prefs.getString(KEY_SORT_ORDER, SORT_NEWEST) ?: SORT_NEWEST)
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    fun setSortOrder(order: String) {
        _sortOrder.value = order
        prefs.edit().putString(KEY_SORT_ORDER, order).apply()
    }

    // ── DAILY HEALTH TIP REMINDER ────────────────────────────────────────────
    private val _dailyReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_DAILY_REMINDER, true))
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled.asStateFlow()

    fun setDailyReminderEnabled(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DAILY_REMINDER, enabled).apply()
    }

    private val _dailyReminderHour = MutableStateFlow(prefs.getInt(KEY_DAILY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR))
    val dailyReminderHour: StateFlow<Int> = _dailyReminderHour.asStateFlow()

    private val _dailyReminderMinute = MutableStateFlow(prefs.getInt(KEY_DAILY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE))
    val dailyReminderMinute: StateFlow<Int> = _dailyReminderMinute.asStateFlow()

    fun setDailyReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        _dailyReminderHour.value = safeHour
        _dailyReminderMinute.value = safeMinute
        prefs.edit()
            .putInt(KEY_DAILY_REMINDER_HOUR, safeHour)
            .putInt(KEY_DAILY_REMINDER_MINUTE, safeMinute)
            .apply()
    }

    fun getFormattedReminderTime(): String {
        return formatReminderTime(_dailyReminderHour.value, _dailyReminderMinute.value)
    }

    fun formatReminderTime(hour: Int, minute: Int): String {
        val period = when (hour) {
            in 5..11 -> "সকাল"
            in 12..14 -> "দুপুর"
            in 15..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        fun toBn(n: Int): String {
            val s = "%02d".format(n)
            return s.map { ch ->
                when (ch) {
                    '0' -> '০'
                    '1' -> '১'
                    '2' -> '২'
                    '3' -> '৩'
                    '4' -> '৪'
                    '5' -> '৫'
                    '6' -> '৬'
                    '7' -> '৭'
                    '8' -> '৮'
                    '9' -> '৯'
                    else -> ch
                }
            }.joinToString("")
        }
        return "$period ${toBn(h12)}:${toBn(minute)} ($amPm)"
    }

    private val _dailyTipOffset = MutableStateFlow(0)
    val dailyTipOffset: StateFlow<Int> = _dailyTipOffset.asStateFlow()

    fun nextDailyTip() {
        _dailyTipOffset.value += 1
    }

    // ── ENCRYPTED & FIRESTORE-SYNCED ADMIN MODE ──────────────────────────────
    private val _isAdminMode = MutableStateFlow(prefs.getBoolean(KEY_ADMIN_UNLOCKED, false))
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    /**
     * One-way salted SHA-256 encryption for admin passwords & recovery keys.
     * Plain-text passwords are never stored in SharedPreferences or Firestore.
     */
    fun encryptSecret(raw: String): String {
        return sha256("HK_ADMIN_SALT_2026_${raw.trim()}")
    }

    fun getAdminPasswordHash(): String {
        val existingHash = prefs.getString(KEY_ADMIN_PASSWORD_HASH, null)
        if (!existingHash.isNullOrBlank()) return existingHash

        // Migrate any legacy plain-text password to encrypted SHA-256 hash and remove plain text
        val legacyPlain = prefs.getString(KEY_LEGACY_ADMIN_PASSWORD, null)
        val initialHash = if (!legacyPlain.isNullOrBlank()) {
            encryptSecret(legacyPlain)
        } else {
            encryptSecret(DEFAULT_ADMIN_PASSWORD)
        }
        prefs.edit()
            .putString(KEY_ADMIN_PASSWORD_HASH, initialHash)
            .remove(KEY_LEGACY_ADMIN_PASSWORD)
            .apply()
        return initialHash
    }

    fun getAdminRecoveryHash(): String {
        val existingHash = prefs.getString(KEY_ADMIN_RECOVERY_HASH, null)
        if (!existingHash.isNullOrBlank()) return existingHash
        val defaultHash = encryptSecret(DEFAULT_RECOVERY_CODE)
        prefs.edit().putString(KEY_ADMIN_RECOVERY_HASH, defaultHash).apply()
        return defaultHash
    }

    fun saveEncryptedAdminCredentials(passwordHash: String, recoveryKeyHash: String? = null) {
        val editor = prefs.edit().putString(KEY_ADMIN_PASSWORD_HASH, passwordHash)
        if (!recoveryKeyHash.isNullOrBlank()) {
            editor.putString(KEY_ADMIN_RECOVERY_HASH, recoveryKeyHash)
        }
        editor.remove(KEY_LEGACY_ADMIN_PASSWORD).apply()
    }

    fun unlockAdminSession() {
        _isAdminMode.value = true
        prefs.edit().putBoolean(KEY_ADMIN_UNLOCKED, true).apply()
    }

    fun verifyAndUnlockAdmin(inputPassword: String, remotePasswordHash: String? = null): Boolean {
        val inputHash = encryptSecret(inputPassword)
        val targetHash = if (!remotePasswordHash.isNullOrBlank()) {
            saveEncryptedAdminCredentials(remotePasswordHash)
            remotePasswordHash
        } else {
            getAdminPasswordHash()
        }
        return if (inputHash == targetHash) {
            unlockAdminSession()
            true
        } else {
            false
        }
    }

    fun verifyRecoveryCode(inputRecoveryCode: String, remoteRecoveryHash: String? = null): Boolean {
        val inputHash = encryptSecret(inputRecoveryCode)
        val defaultMasterHash = encryptSecret(DEFAULT_RECOVERY_CODE)
        val savedHash = if (!remoteRecoveryHash.isNullOrBlank()) {
            prefs.edit().putString(KEY_ADMIN_RECOVERY_HASH, remoteRecoveryHash).apply()
            remoteRecoveryHash
        } else {
            getAdminRecoveryHash()
        }
        return inputHash == savedHash || inputHash == defaultMasterHash
    }

    fun lockAdminMode() {
        _isAdminMode.value = false
        prefs.edit().putBoolean(KEY_ADMIN_UNLOCKED, false).apply()
    }

    fun changeAdminPassword(newPassword: String): Boolean {
        if (newPassword.trim().length < 4) return false
        val newHash = encryptSecret(newPassword)
        saveEncryptedAdminCredentials(newHash)
        return true
    }

    // ── IP / DEVICE REACTION IDENTIFIER ──────────────────────────────────────
    /**
     * Resolves the client's public IP address (cached per network session)
     * and hashes it into a safe Firestore document key so each IP can react/verify once per post.
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

    fun getDeviceAuthorId(): String {
        return getFallbackDeviceId()
    }

    fun getMySubmittedDocIds(): Set<String> {
        return prefs.getStringSet(KEY_MY_SUBMISSIONS, emptySet()) ?: emptySet()
    }

    fun addMySubmittedDocId(docIdOrKey: String) {
        if (docIdOrKey.isBlank()) return
        val current = getMySubmittedDocIds().toMutableSet()
        current.add(docIdOrKey.trim())
        prefs.edit().putStringSet(KEY_MY_SUBMISSIONS, current).apply()
    }

    fun isMySubmittedPost(postKey: String, firebaseDocId: String): Boolean {
        val mine = getMySubmittedDocIds()
        return (postKey.isNotBlank() && postKey in mine) ||
               (firebaseDocId.isNotBlank() && firebaseDocId in mine)
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

        private const val KEY_THEME_MODE = "reading_theme_mode"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_SEPIA = "sepia"
        const val THEME_DARK = "dark"

        private const val KEY_SORT_ORDER = "article_sort_order_v2"
        const val SORT_POPULAR = "popular"
        const val SORT_NEWEST = "newest"
        const val SORT_OLDEST = "oldest"

        private const val KEY_DAILY_REMINDER = "daily_tip_reminder_enabled"
        private const val KEY_DAILY_REMINDER_HOUR = "daily_tip_reminder_hour"
        private const val KEY_DAILY_REMINDER_MINUTE = "daily_tip_reminder_minute"
        const val DEFAULT_REMINDER_HOUR = 8 // Default 8:00 AM (সকাল ৮:০০ টা)
        const val DEFAULT_REMINDER_MINUTE = 0

        private const val KEY_ADMIN_UNLOCKED = "is_admin_unlocked"
        private const val KEY_LEGACY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_ADMIN_PASSWORD_HASH = "admin_password_sha256_hash"
        private const val KEY_ADMIN_RECOVERY_HASH = "admin_recovery_sha256_hash"
        const val DEFAULT_ADMIN_PASSWORD = "admin123"
        const val DEFAULT_RECOVERY_CODE = "doctor2026"

        private const val KEY_CACHED_IP = "cached_public_ip"
        private const val KEY_IP_FETCH_TIME = "cached_ip_fetch_time"
        private const val KEY_DEVICE_FALLBACK_ID = "device_fallback_id"
        private const val KEY_MY_SUBMISSIONS = "my_submitted_articles_keys"
    }
}
