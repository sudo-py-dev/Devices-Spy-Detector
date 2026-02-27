package com.spydetect.edapps.ui.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spydetect.edapps.R
import com.spydetect.edapps.data.repository.PreferenceRepository
import com.spydetect.edapps.data.repository.SpyRepository
import com.spydetect.edapps.ui.main.LogExporter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    val mainLayout = findViewById<View>(R.id.settings_root)
    val appBar = findViewById<AppBarLayout>(R.id.appbar)
    val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    WindowCompat.setDecorFitsSystemWindows(window, false)

    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

    val container = findViewById<FrameLayout>(R.id.settings_container)
    ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

      appBar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
      container.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

      insets
    }

    if (savedInstanceState == null) {
      supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings_container, SettingsFragment())
        .commit()
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
  }

  @AndroidEntryPoint
  class SettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var preferencesManager: PreferenceRepository
    @Inject lateinit var spyRepository: SpyRepository

    companion object {
      private const val SENSITIVITY_LOW_MAX = 3
      private const val SENSITIVITY_MEDIUM_MAX = 7
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = SecurePreferenceDataStore(preferencesManager)

      setPreferencesFromResource(R.xml.preferences, rootKey)

      val cooldownPref = findPreference<SeekBarPreference>("cooldown_sec")
      val rssiPref = findPreference<SeekBarPreference>("sensitivity_level")
      val languagePref = findPreference<Preference>("app_language")

      cooldownPref?.summaryProvider =
        Preference.SummaryProvider<SeekBarPreference> { preference ->
          try {
            getString(R.string.summary_cooldown, preference.value)
          } catch (e: Exception) {
            Log.e("SettingsActivity", "Error in cooldown summary provider", e)
            "Error"
          }
        }

      cooldownPref?.setOnPreferenceChangeListener { pref, newValue ->
        try {
          val value = newValue as? Int ?: return@setOnPreferenceChangeListener false
          (pref as SeekBarPreference).value = value
          pref.summary = getString(R.string.summary_cooldown, value)
          true
        } catch (e: Exception) {
          Log.e("SettingsActivity", "Error in cooldown preference change", e)
          false // Prevent invalid change
        }
      }

      rssiPref?.summaryProvider =
        Preference.SummaryProvider<SeekBarPreference> { preference ->
          try {
            val resId =
              when (preference.value) {
                in 1..SENSITIVITY_LOW_MAX -> R.string.sensitivity_low
                in (SENSITIVITY_LOW_MAX + 1)..SENSITIVITY_MEDIUM_MAX -> R.string.sensitivity_medium
                else -> R.string.sensitivity_high
              }
            getString(resId)
          } catch (e: Exception) {
            Log.e("SettingsActivity", "Error in sensitivity summary provider", e)
            "Error"
          }
        }

      rssiPref?.setOnPreferenceChangeListener { pref, newValue ->
        try {
          val value = newValue as? Int ?: return@setOnPreferenceChangeListener false
          (pref as SeekBarPreference).value = value
          val resId =
            when (value) {
              in 1..SENSITIVITY_LOW_MAX -> R.string.sensitivity_low
              in (SENSITIVITY_LOW_MAX + 1)..SENSITIVITY_MEDIUM_MAX -> R.string.sensitivity_medium
              else -> R.string.sensitivity_high
            }
          pref.summary = getString(resId)
          true
        } catch (e: Exception) {
          Log.e("SettingsActivity", "Error in sensitivity preference change", e)
          false // Prevent invalid change
        }
      }

      fun refreshLanguageSummary() {
        languagePref?.let {
          val entries = resources.getStringArray(R.array.pref_language_entries)
          val values = resources.getStringArray(R.array.pref_language_values)
          val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
          val normalized = if (currentTag.isBlank()) "" else currentTag
          val index = values.indexOf(normalized).coerceAtLeast(0)
          it.summary = entries[index]
        }
      }

      languagePref?.setOnPreferenceClickListener {
        showLanguageDialog()
        true
      }


      findPreference<Preference>("share_history")?.setOnPreferenceClickListener {
        exportLogFromSettings()
        true
      }

      findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
        showClearLogDialog()
        true
      }

      refreshLanguageSummary()
    }

    private fun exportLogFromSettings() {
      val logExporter = LogExporter(requireContext())

      lifecycleScope.launch {
        spyRepository.allDetections.collectLatest { events ->
          if (events.isEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_to_export, Toast.LENGTH_SHORT).show()
          } else {
            logExporter.exportLog(
              events,
              { file -> logExporter.shareFile(file) },
              { _ ->
                Toast.makeText(requireContext(), R.string.toast_export_error, Toast.LENGTH_SHORT)
                  .show()
              }
            )
          }
          this@launch.cancel()
        }
      }
    }

    private fun showClearLogDialog() {

      lifecycleScope.launch {
        spyRepository.allDetections.first().let { events ->
          if (events.isEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_to_clear, Toast.LENGTH_SHORT).show()
          } else {
            MaterialAlertDialogBuilder(requireContext())
              .setTitle(R.string.dialog_clear_log_title)
              .setMessage(R.string.dialog_clear_log_message)
              .setPositiveButton(R.string.dialog_action_clear_confirm) { _, _ ->
                lifecycleScope.launch {
                  spyRepository.clearAll()
                  Toast.makeText(requireContext(), R.string.toast_log_cleared, Toast.LENGTH_SHORT)
                    .show()
                }
              }
              .setNegativeButton(R.string.dialog_action_cancel, null)
              .show()
          }
        }
      }
    }

    override fun onResume() {
      super.onResume()
    }

    private fun showLanguageDialog() {
      val entries = resources.getStringArray(R.array.pref_language_entries)
      val values = resources.getStringArray(R.array.pref_language_values)
      val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
      val normalized = if (currentTag.isBlank()) "" else currentTag
      val currentIndex = values.indexOf(normalized).coerceAtLeast(0)

      MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.pref_language_title)
        .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
          val langTag = values[which]
          applyAppLanguage(langTag)
          dialog.dismiss()
          Handler(Looper.getMainLooper()).post { activity?.recreate() }
        }
        .show()
    }

    private fun applyAppLanguage(tag: String) {
      val locales =
        if (tag.isBlank()) {
          LocaleListCompat.getEmptyLocaleList()
        } else {
          LocaleListCompat.forLanguageTags(tag)
        }
      AppCompatDelegate.setApplicationLocales(locales)
    }
  }

  private class SecurePreferenceDataStore(private val repository: PreferenceRepository) :
    PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
      repository.putString(key, value)
    }

    override fun getString(key: String, defValue: String?): String? = repository.getString(key, defValue)

    override fun putInt(key: String, value: Int) {
      repository.putInt(key, value)
    }

    override fun getInt(key: String, defValue: Int): Int = repository.getInt(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
      repository.putBoolean(key, value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
      repository.getBoolean(key, defValue)

    override fun putLong(key: String, value: Long) {
      repository.putLong(key, value)
    }

    override fun getLong(key: String, defValue: Long): Long = repository.getLong(key, defValue)
  }
}
