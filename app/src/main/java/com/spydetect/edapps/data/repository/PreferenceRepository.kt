package com.spydetect.edapps.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class PreferenceRepository(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("secure_prefs_v2", Context.MODE_PRIVATE)
  private val aead: Aead

  init {
    AeadConfig.register()
    val keysetHandle = AndroidKeysetManager.Builder()
      .withSharedPref(context, "tink_keyset", "tink_prefs")
      .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
      .withMasterKeyUri("android-keystore://pref_master_key")
      .build()
      .keysetHandle
    aead = keysetHandle.getPrimitive(Aead::class.java)
  }

  fun putString(key: String, value: String?) {
    prefs.edit().putString(key, value?.let { encrypt(it) }).apply()
  }

  fun getString(key: String, defValue: String?): String? {
    return decrypt(prefs.getString(key, null)) ?: defValue
  }

  fun putInt(key: String, value: Int) {
    putString(key, value.toString())
  }

  fun getInt(key: String, defValue: Int): Int {
    return getString(key, null)?.toIntOrNull() ?: defValue
  }

  fun putBoolean(key: String, value: Boolean) {
    putString(key, value.toString())
  }

  fun getBoolean(key: String, defValue: Boolean): Boolean {
    return getString(key, null)?.toBooleanStrictOrNull() ?: defValue
  }

  fun putLong(key: String, value: Long) {
    putString(key, value.toString())
  }

  fun getLong(key: String, defValue: Long): Long {
    return getString(key, null)?.toLongOrNull() ?: defValue
  }

  private fun encrypt(value: String): String {
    val ciphertext = aead.encrypt(value.toByteArray(), null)
    return Base64.encodeToString(ciphertext, Base64.DEFAULT)
  }

  private fun decrypt(value: String?): String? {
    if (value == null) return null
    return try {
      val ciphertext = Base64.decode(value, Base64.DEFAULT)
      val plaintext = aead.decrypt(ciphertext, null)
      String(plaintext)
    } catch (e: Exception) {
      null
    }
  }

  companion object {
    private const val KEY_SENSITIVITY_LEVEL = "sensitivity_level"
    private const val KEY_COOLDOWN_SEC = "cooldown_sec"
    private const val KEY_FOREGROUND_SERVICE = "foreground_service"
    private const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val KEY_COMPANY_ID_NAMES = "company_id_names"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val RSSI_THRESHOLD_MIN = -100
    private const val RSSI_THRESHOLD_MAX = -40
    private const val SENSITIVITY_MIN = 1
    private const val SENSITIVITY_MAX = 10


    private const val DEFAULT_SENSITIVITY_LEVEL = 5
    private const val DEFAULT_COOLDOWN_SEC = 60
    private const val MS_PER_SEC = 1000L
    private const val DEFAULT_FOREGROUND_SERVICE = true
    private const val DEFAULT_NOTIFICATIONS = true
    private const val DEFAULT_LOGGING_ENABLED = true
  }

  var rssiThreshold: Int
    get() {
      val level = getInt(KEY_SENSITIVITY_LEVEL, DEFAULT_SENSITIVITY_LEVEL)
      // Map 1-10 to -40 to -100
      // level 1 -> -40 (Least sensitive, must be very close)
      // level 10 -> -100 (Most sensitive, detects far away)
      return RSSI_THRESHOLD_MAX -
        (level - 1) * (RSSI_THRESHOLD_MAX - RSSI_THRESHOLD_MIN) /
          (SENSITIVITY_MAX - SENSITIVITY_MIN)
    }
    private set(value) {}

  var cooldownMs: Long
    get() = getInt(KEY_COOLDOWN_SEC, DEFAULT_COOLDOWN_SEC).toLong() * MS_PER_SEC
    set(value) = putInt(KEY_COOLDOWN_SEC, (value / MS_PER_SEC).toInt())

  var foregroundServiceEnabled: Boolean
    get() = getBoolean(KEY_FOREGROUND_SERVICE, DEFAULT_FOREGROUND_SERVICE)
    set(value) = putBoolean(KEY_FOREGROUND_SERVICE, value)

  var notificationsEnabled: Boolean
    get() = getBoolean(KEY_ENABLE_NOTIFICATIONS, DEFAULT_NOTIFICATIONS)
    set(value) = putBoolean(KEY_ENABLE_NOTIFICATIONS, value)

  var loggingEnabled: Boolean
    get() = getBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED)
    set(value) = putBoolean(KEY_LOGGING_ENABLED, value)

  val loggingEnabledFlow: Flow<Boolean> =
    callbackFlow {
        val listener =
          SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LOGGING_ENABLED) {
              trySend(loggingEnabled)
            }
          }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
      }
      .onStart { emit(loggingEnabled) }

  var companyIdNames: Map<Int, String>
    get() {
      val raw = getString(KEY_COMPANY_ID_NAMES, "") ?: ""
      if (raw.isBlank()) return emptyMap()
      return raw
        .split("|")
        .mapNotNull { entry ->
          val parts = entry.split(":", limit = 2)
          if (parts.size == 2) {
            val id = parts[0].trim().toIntOrNull()
            val name = parts[1].trim()
            if (id != null && name.isNotEmpty()) id to name else null
          } else null
        }
        .toMap()
    }
    set(value) {
      val encoded = value.entries.joinToString("|") { "${it.key}:${it.value}" }
      putString(KEY_COMPANY_ID_NAMES, encoded)
    }

  fun setCompanyIdName(id: Int, name: String?) {
    val current = companyIdNames.toMutableMap()
    if (name == null) current.remove(id) else current[id] = name
    companyIdNames = current
  }

  fun getCompanyIdName(id: Int): String? = companyIdNames[id]

  var themeMode: Int
    get() = getInt(KEY_THEME_MODE, 0)
    set(value) = putInt(KEY_THEME_MODE, value)

  var appLanguage: String?
    get() = getString(KEY_APP_LANGUAGE, null)
    set(value) = putString(KEY_APP_LANGUAGE, value)

  var onboardingCompleted: Boolean
    get() = getBoolean(KEY_ONBOARDING_COMPLETED, false)
    set(value) = putBoolean(KEY_ONBOARDING_COMPLETED, value)

  internal val encryptedPrefs: SharedPreferences
    get() = prefs

  fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
  }
}
