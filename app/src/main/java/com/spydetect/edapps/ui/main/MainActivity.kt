package com.spydetect.edapps.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.spydetect.edapps.R
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.data.repository.PreferenceRepository
import com.spydetect.edapps.databinding.ActivityMainBinding
import com.spydetect.edapps.service.SpyScannerService
import com.spydetect.edapps.ui.about.AboutActivity
import com.spydetect.edapps.ui.settings.SettingsActivity
import com.spydetect.edapps.util.PermissionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.activity.viewModels

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

  private var _binding: ActivityMainBinding? = null
  private val binding
    get() = checkNotNull(_binding)

  @Inject lateinit var preferencesManager: PreferenceRepository
  private val viewModel: MainViewModel by viewModels()
  @Inject lateinit var trackerRepo: com.spydetect.edapps.data.repository.TrackerRepository

  private var _spyEventAdapter: SpyEventAdapter? = null
  private val spyEventAdapter
    get() = checkNotNull(_spyEventAdapter)

  private var serviceBound = false
  private var spyScannerService: SpyScannerService? = null
  private var startScanAfterBind = false

  private val bluetoothHelper: BluetoothManagerHelper by lazy {
    BluetoothManagerHelper(
      activity = this,
      bluetoothPermissionLauncher,
      enableBluetoothLauncher,
      { startScanningProcess() },
      { msg -> showSnackbar(msg) }
    )
  }


  private val serviceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as SpyScannerService.LocalBinder
        spyScannerService = binder.getService()
        serviceBound = true
        viewModel.updateServiceStatus(true)

        spyScannerService?.toggleDetectionListener(detectionListener, true)
        if (startScanAfterBind) {
          startScanAfterBind = false
          spyScannerService?.startSpyScan()
        }
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        spyScannerService?.toggleDetectionListener(detectionListener, false)
        spyScannerService = null
        serviceBound = false
        viewModel.updateServiceStatus(false)
      }
    }

  private val detectionListener: (SpyEvent) -> Unit = { event ->
    runOnUiThread { viewModel.addSpyEvent(event) }
  }

  private val bluetoothPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      if (permissions.all { it.value }) {
        bluetoothHelper.checkBluetoothEnabled()
      } else {
        showPermissionDeniedDialog()
      }
    }

  private val enableBluetoothLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      bluetoothHelper.handleBluetoothResult(result.resultCode)
    }

  private fun setupEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      binding.appbar.setPadding(bars.left, bars.top, bars.right, 0)
      binding.radarView.setPadding(0, bars.top / 2, 0, 0)

      val params =
        binding.layoutScanningActions.layoutParams as android.view.ViewGroup.MarginLayoutParams
      params.bottomMargin = bars.bottom + (BOTTOM_MARGIN_DP * resources.displayMetrics.density).toInt()
      binding.layoutScanningActions.layoutParams = params

      insets
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupEdgeToEdge()
    initViews()
    setSupportActionBar(binding.toolbar)
    observeViewModel()

    lifecycleScope.launch {
      try {
        if (::preferencesManager.isInitialized) {
          trackerRepo.seedIfNeeded()
          checkBatteryOptimization()
          if (!preferencesManager.onboardingCompleted) {
            showOnboardingDialog()
          }
        } else {
          Log.w("MainActivity", "preferencesManager not initialized, skipping lifecycle operations")
        }
      } catch (e: Exception) {
        Log.e("MainActivity", "Error in lifecycle operations", e)
      }
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    // Apply theme after all Hilt injections are definitely complete
    applyThemeSafely()
  }

  private fun applyThemeSafely() {
    try {
      if (::preferencesManager.isInitialized) {
        preferencesManager.registerListener(this)
        applyAppTheme()
      } else {
        Log.w("MainActivity", "preferencesManager not initialized, using default theme")
        applyDefaultTheme()
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error applying theme", e)
      applyDefaultTheme()
    }
  }

  private fun initViews() {
    supportActionBar?.setDisplayShowTitleEnabled(false)

    _spyEventAdapter = SpyEventAdapter { event ->
      copyToClipboard(event.toLogString(this))
    }
    binding.rvSpyEvents.apply {
      layoutManager = LinearLayoutManager(this@MainActivity)
      adapter = spyEventAdapter
    }

    binding.btnToggleScan.setOnClickListener {
      try {
        if (serviceBound && spyScannerService?.isScanning == true) {
          stopSpyScan()
        } else {
          bluetoothHelper.requestPermissionsAndScan()
        }
      } catch (e: Exception) {
        Log.e("MainActivity", "Error in scan button click", e)
        showSnackbar("Error: Unable to start scanning")
      }
    }

    binding.btnMoveToBackground.setOnClickListener {
      try {
        moveAppToBackground()
      } catch (e: Exception) {
        Log.e("MainActivity", "Error moving to background", e)
        showSnackbar("Error: Unable to move to background")
      }
    }

    binding.btnDisableBatteryOptimization.setOnClickListener {
      try {
        PermissionManager.requestDisableBatteryOptimization(this)
      } catch (e: Exception) {
        Log.e("MainActivity", "Error requesting battery optimization", e)
        showSnackbar("Error: Unable to open settings")
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkBatteryOptimization()
  }

  private fun observeViewModel() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.uiState.collectLatest { state ->
            updateScanningUI(state is MainUiState.Scanning)
          }
        }
        launch {
          viewModel.allEvents.collectLatest { events ->
            spyEventAdapter.submitList(events)
            updateLogVisibility(events.isEmpty())
            if (events.isNotEmpty()) {
              binding.rvSpyEvents.smoothScrollToPosition(events.size - 1)
            }
          }
        }
      }
    }
  }

  private fun updateLogVisibility(isLogEmpty: Boolean) {
    binding.rvSpyEvents.visibility = if (isLogEmpty) View.GONE else View.VISIBLE
    binding.viewEmptyState.visibility = if (isLogEmpty) View.VISIBLE else View.GONE
  }

  private fun checkBatteryOptimization() {
    if (!::preferencesManager.isInitialized) {
      Log.w("MainActivity", "preferencesManager not initialized, skipping battery optimization check")
      return
    }
    
    val isServiceEnabled = preferencesManager.foregroundServiceEnabled
    val isRestricted = !PermissionManager.isBatteryOptimizationDisabled(this)

    binding.cvBatteryOptimization.visibility =
      if (isServiceEnabled && isRestricted) {
        View.VISIBLE
      } else {
        View.GONE
      }
  }

  private fun copyToClipboard(text: String) {
    try {
      val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
      val clip = android.content.ClipData.newPlainText(getString(R.string.clipboard_label_log), text)
      clipboard.setPrimaryClip(clip)
      showSnackbar(getString(R.string.clipboard_copied))
    } catch (e: Exception) {
      Log.e("MainActivity", "Error copying to clipboard", e)
      showSnackbar("Error: Unable to copy to clipboard")
    }
  }

  private fun showSnackbar(
    message: String,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
  ) {
    try {
      if (!isFinishing && !isDestroyed) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.anchorView = binding.layoutScanningActions
        if (actionLabel != null && action != null) {
          snackbar.setAction(actionLabel) { action() }
        }
        snackbar.show()
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error showing snackbar", e)
    }
  }

  private fun updateScanningUI(isScanning: Boolean) {
    binding.btnToggleScan.text =
      if (isScanning) getString(R.string.button_stop_scanning)
      else getString(R.string.button_start_scanning)
    binding.btnToggleScan.setIconResource(
      if (isScanning) R.drawable.ic_stop_24 else R.drawable.ic_play_arrow_24
    )

    binding.radarView.setScanning(isScanning)
    binding.ivEmptyStateCenter.visibility = if (isScanning) View.GONE else View.VISIBLE

    binding.btnMoveToBackground.visibility = if (isScanning) View.VISIBLE else View.GONE
  }

  private fun startScanningProcess() {
    if (!::preferencesManager.isInitialized) {
      Log.e("MainActivity", "preferencesManager not initialized, cannot start scanning")
      showSnackbar("Error: Preferences not initialized")
      return
    }
    
    if (preferencesManager.foregroundServiceEnabled) {
      val intent =
        Intent(this, SpyScannerService::class.java).apply {
          action = SpyScannerService.ACTION_START_SCAN
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
      } else {
        startService(intent)
      }
    } else {
      startScanAfterBind = true
    }
    bindService(
      Intent(this, SpyScannerService::class.java),
      serviceConnection,
      Context.BIND_AUTO_CREATE
    )
  }

  private fun stopSpyScan() {
    if (serviceBound) {
      if (::preferencesManager.isInitialized && preferencesManager.foregroundServiceEnabled) {
        startService(
          Intent(this, SpyScannerService::class.java).apply {
            action = SpyScannerService.ACTION_STOP_SCAN
          }
        )
      } else {
        spyScannerService?.stopSpyScan()
      }
      unbindService(serviceConnection)
      serviceBound = false
      viewModel.updateServiceStatus(false)
    }
  }

  private fun showPermissionDeniedDialog() {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.dialog_permissions_title)
      .setMessage(R.string.dialog_permissions_message)
      .setPositiveButton(R.string.dialog_action_open_settings) { _, _ ->
        startActivity(
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
          }
        )
      }
      .setNegativeButton(R.string.dialog_action_cancel, null)
      .show()
  }

  private fun showOnboardingDialog() {
    try {
      if (!::preferencesManager.isInitialized) {
        Log.w("MainActivity", "preferencesManager not initialized, skipping onboarding")
        return
      }
      
      val dialogBinding =
        com.spydetect.edapps.databinding.DialogOnboardingBinding.inflate(layoutInflater)
      val dialog =
        MaterialAlertDialogBuilder(this, R.style.Dialog_SpyDetector)
          .setView(dialogBinding.root)
          .setCancelable(false)
          .create()

      dialogBinding.cbAgree.setOnCheckedChangeListener { _, isChecked ->
        dialogBinding.btnAccept.isEnabled = isChecked
      }

      dialogBinding.btnAccept.setOnClickListener {
        try {
          preferencesManager.onboardingCompleted = true
          dialog.dismiss()
        } catch (e: Exception) {
          Log.e("MainActivity", "Error setting onboarding completed", e)
        }
      }

      dialog.show()
    } catch (e: Exception) {
      Log.e("MainActivity", "Error showing onboarding dialog", e)
    }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    try {
      if (key == "foreground_service") {
        checkBatteryOptimization()
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error in preference change listener", e)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_theme -> {
        showThemeDialog()
        true
      }
      R.id.action_settings -> {
        val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        startActivity(Intent(this, SettingsActivity::class.java), options.toBundle())
        true
      }
      R.id.action_device_id_manager -> {
        val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        startActivity(
          Intent(this, com.spydetect.edapps.ui.deviceids.DeviceIdManagerActivity::class.java),
          options.toBundle()
        )
        true
      }
      R.id.action_about -> {

        val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        startActivity(Intent(this, AboutActivity::class.java), options.toBundle())
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun showThemeDialog() {
    try {
      if (!::preferencesManager.isInitialized) {
        showSnackbar("Error: Preferences not available")
        return
      }
      
      val themes =
        arrayOf(
          getString(R.string.theme_system),
          getString(R.string.theme_light),
          getString(R.string.theme_dark),
        )
      MaterialAlertDialogBuilder(this, R.style.Dialog_SpyDetector)
        .setTitle(R.string.action_theme)
        .setSingleChoiceItems(themes, preferencesManager.themeMode) { dialog, which ->
          preferencesManager.themeMode = which
          applyAppTheme()
          dialog.dismiss()
          recreate()
        }
        .show()
    } catch (e: Exception) {
      Log.e("MainActivity", "Error showing theme dialog", e)
      showSnackbar("Error: Unable to show theme dialog")
    }
  }

  private fun applyAppTheme() {
    try {
      if (!::preferencesManager.isInitialized) {
        Log.w("MainActivity", "preferencesManager not initialized in applyAppTheme")
        return
      }
      
      val mode =
        when (preferencesManager.themeMode) {
          1 -> AppCompatDelegate.MODE_NIGHT_NO
          2 -> AppCompatDelegate.MODE_NIGHT_YES
          else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
      AppCompatDelegate.setDefaultNightMode(mode)
    } catch (e: Exception) {
      Log.e("MainActivity", "Error applying theme", e)
      applyDefaultTheme()
    }
  }

  private fun applyDefaultTheme() {
    // Apply system default theme when preferencesManager is not available
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
  }

  private fun moveAppToBackground() {
    android.widget.Toast.makeText(this, R.string.toast_scanning_in_background, android.widget.Toast.LENGTH_SHORT).show()
    val homeIntent =
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    startActivity(homeIntent)
  }

  override fun onDestroy() {
    super.onDestroy()
    preferencesManager.unregisterListener(this)
    if (serviceBound) {
      spyScannerService?.toggleDetectionListener(detectionListener, false)
      unbindService(serviceConnection)
    }
    _binding = null
    _spyEventAdapter = null
  }

  companion object {
    private const val BOTTOM_MARGIN_DP = 24
  }
}
