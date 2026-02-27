package com.spydetect.edapps.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.data.repository.PreferenceRepository
import com.spydetect.edapps.data.repository.SpyRepository
import com.spydetect.edapps.scanner.ISpyScanner
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.spydetect.edapps.di.ScannerFactory

@AndroidEntryPoint
class SpyScannerService : Service() {

  private val binder = LocalBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @Inject lateinit var preferencesManager: PreferenceRepository
  @Inject lateinit var spyRepository: SpyRepository
  @Inject lateinit var notificationHelper: NotificationHelper
  @Inject lateinit var scanningStatusRepository: com.spydetect.edapps.data.repository.ScannerStatusRepository
  @Inject lateinit var scannerFactory: ScannerFactory
  private var spyScanner: ISpyScanner? = null

  private val bluetoothStateReceiver by lazy { ScannerStateReceiver { stopScanningAndService() } }

  private fun stopScanningAndService() {
    stopSpyScan()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION") stopForeground(true)
    }
    stopSelf()
  }

  private val preferenceChangeListener =
    android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      when (key) {
        "rssi_threshold" -> {
          val newThreshold = preferencesManager.rssiThreshold
          spyScanner?.updateRssiThreshold(newThreshold)
        }
        "foreground_service" -> {
          // If scanning, we might need to restart as foreground/background
          if (isScanning) {
            val scanMode =
              if (preferencesManager.foregroundServiceEnabled) {
                no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_BALANCED
              } else {
                no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY
              }
            stopSpyScan()
            startSpyScan(scanMode)
          }
        }
      }
    }

  private val detectionListeners = mutableListOf<(SpyEvent) -> Unit>()
  private val lastNotificationTime = AtomicLong(0L)

  inner class LocalBinder : Binder() {
    fun getService(): SpyScannerService = this@SpyScannerService
  }

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "Service created")

    val filter =
      android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
    registerReceiver(bluetoothStateReceiver, filter)

    preferencesManager.registerListener(preferenceChangeListener)
  }

  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START_SCAN -> startForegroundService()
      ACTION_STOP_SCAN -> stopScanningAndService()
    }
    return START_STICKY
  }

  private fun startForegroundService() {
    val notification = notificationHelper.createServiceNotification()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NotificationHelper.NOTIFICATION_ID_SERVICE,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
      )
    } else {
      startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
    }

    startSpyScan(ScanSettings.SCAN_MODE_BALANCED)
  }

  fun startSpyScan(scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) {
    if (spyScanner?.isScanning?.value == true) {
      Log.w(TAG, "Already scanning")
      return
    }

    scanningStatusRepository.clearDiscoveredIds()

    val rssiThreshold = preferencesManager.rssiThreshold

    spyScanner = scannerFactory.create(rssiThreshold, detectionListener)

    serviceScope.launch {
      val success = spyScanner?.startScanning(scanMode) ?: false
      if (success) {
        Log.i(TAG, "Scanning started successfully")
        scanningStatusRepository.setScanning(true)
      } else {

        Log.e(TAG, "Failed to start scanning")
      }
    }
  }

  fun stopSpyScan() {
    spyScanner?.stopScanning()
    spyScanner = null
    scanningStatusRepository.setScanning(false)
  }

  private val detectionListener: (SpyEvent) -> Unit = { event ->
    if (preferencesManager.loggingEnabled) {
      serviceScope.launch { spyRepository.insertDetection(event) }
    }

    detectionListeners.forEach { it(event) }

    val currentTime = System.currentTimeMillis()
    val cooldown = preferencesManager.cooldownMs

    val lastTime = lastNotificationTime.get()
    if (currentTime - lastTime >= cooldown) {
      if (lastNotificationTime.compareAndSet(lastTime, currentTime)) {
        if (preferencesManager.notificationsEnabled) {
          notificationHelper.showDetectionNotification(event)
        }
      }
    } else {
      Log.d(TAG, "Detection within cooldown period, notification suppressed")
    }
  }

  fun toggleDetectionListener(listener: (SpyEvent) -> Unit, add: Boolean) {
    if (add) detectionListeners.add(listener) else detectionListeners.remove(listener)
  }

  val isScanning: Boolean
    get() = spyScanner?.isScanning?.value == true

  override fun onDestroy() {
    super.onDestroy()
    preferencesManager.unregisterListener(preferenceChangeListener)
    stopSpyScan()
    serviceScope.cancel()
    unregisterReceiver(bluetoothStateReceiver)
    Log.d(TAG, "Service destroyed")
  }

  companion object {
    private const val TAG = "SpyScannerService"
    const val ACTION_START_SCAN = "com.spydetect.edapps.START_SCAN"
    const val ACTION_STOP_SCAN = "com.spydetect.edapps.STOP_SCAN"
  }
}
