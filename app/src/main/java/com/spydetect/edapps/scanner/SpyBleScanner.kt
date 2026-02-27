package com.spydetect.edapps.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.spydetect.edapps.R
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.scanner.protocol.SpyDetectionResult
import com.spydetect.edapps.scanner.protocol.SpyProtocol
import com.spydetect.edapps.util.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class SpyBleScanner(
  private val context: Context,
  private var rssiThreshold: Int,
  private val protocols: List<SpyProtocol>,
  private val onCompanyIdDiscovered: ((Int, Int, String?) -> Unit)? = null,
  private val onDeviceDetected: (SpyEvent) -> Unit
) : ISpyScanner {

  private val scanner = BluetoothLeScannerCompat.getScanner()
  private val _isScanning = MutableStateFlow(false)
  override val isScanning: StateFlow<Boolean> = _isScanning

  private val scanCallback =
    object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        processScanResult(result)
      }

      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { processScanResult(it) }
      }

      override fun onScanFailed(errorCode: Int) {
        Log.e(TAG, "Scan failed with error code: $errorCode")
        _isScanning.value = false
      }
    }

  @SuppressLint("MissingPermission")
  override fun startScanning(scanMode: Int): Boolean {
    if (_isScanning.value) {
      Log.w(TAG, "Scanning already in progress")
      return false
    }

    val settings =
      ScanSettings.Builder()
        .setScanMode(scanMode)
        .setReportDelay(
          if (scanMode == ScanSettings.SCAN_MODE_LOW_POWER) SCAN_REPORT_DELAY_MS else 0L
        )
        .setUseHardwareBatchingIfSupported(true)
        .build()

    val success =
      try {
        scanner.startScan(null, settings, scanCallback)
        _isScanning.value = true
        Log.i(TAG, "Scanning started (RSSI threshold: $rssiThreshold dBm)")
        true
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Illegal state during scan", e)
        false
      } catch (e: SecurityException) {
        Log.e(TAG, "Missing permissions for scan", e)
        false
      }
    return success
  }

  @Suppress("TooGenericExceptionCaught")
  override fun stopScanning() {
    if (!_isScanning.value) return
    try {
      scanner.stopScan(scanCallback)
      _isScanning.value = false
      Log.i(TAG, "Scanning stopped")
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping scan", e)
    }
  }

  override fun updateRssiThreshold(newThreshold: Int) {
    this.rssiThreshold = newThreshold
    Log.i(TAG, "RSSI threshold updated to $newThreshold dBm")
  }

  private fun processScanResult(result: ScanResult) {
    val deviceAddress = result.device.address
    if (result.rssi < rssiThreshold) return

    val deviceName = getDeviceName(result)
    val manufacturerData = result.scanRecord?.manufacturerSpecificData

    if (manufacturerData != null && manufacturerData.size() > 0) {
      handleManufacturerData(result, deviceName, manufacturerData)
    } else {
      handleNameOnlyDetection(result, deviceName)
    }
  }

  private fun getDeviceName(result: ScanResult): String? {
    val canReadDeviceIdentity =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PermissionManager.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
      } else true

    return try {
      when {
        !result.scanRecord?.deviceName.isNullOrBlank() -> result.scanRecord?.deviceName
        canReadDeviceIdentity -> result.device.name ?: result.device.address
        else -> null
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException while reading device identity", e)
      null
    }
  }

  private fun handleManufacturerData(
    result: ScanResult,
    deviceName: String?,
    manufacturerData: android.util.SparseArray<ByteArray>
  ) {
    val companyId = manufacturerData.keyAt(0)
    val data = manufacturerData.valueAt(0)
    val manufacturerDataHex = data.joinToString("") { HEX_FORMAT.format(it) }

    val detectionResult =
      protocols
        .asSequence()
        .map { it.identify(context, companyId, deviceName) }
        .find { it.isDetected } ?: SpyDetectionResult(isDetected = false)

    val discoveredProtocol =
      protocols.find { it.identify(context, companyId, deviceName).isDetected }
    val discoveredCompanyName = discoveredProtocol?.getCompanyName(context, companyId)
    onCompanyIdDiscovered?.invoke(companyId, result.rssi, discoveredCompanyName)

    if (detectionResult.isDetected) {
      val event =
        createSpyEvent(result, deviceName, companyId, manufacturerDataHex, detectionResult)
      Log.d(TAG, "Spy device detected: ${deviceName ?: "Unknown"} (${result.rssi} dBm)")
      onDeviceDetected(event)
    }
  }

  private fun handleNameOnlyDetection(result: ScanResult, deviceName: String?) {
    val detectionResult =
      protocols.asSequence().map { it.identify(context, null, deviceName) }.find { it.isDetected }
        ?: SpyDetectionResult(isDetected = false)

    if (detectionResult.isDetected) {
      val event = createSpyEvent(result, deviceName, null, null, detectionResult)
      Log.d(TAG, "Spy device detected by name: ${deviceName ?: "Unknown"} (${result.rssi} dBm)")
      onDeviceDetected(event)
    }
  }

  private fun createSpyEvent(
    result: ScanResult,
    deviceName: String?,
    companyId: Int?,
    manufacturerDataHex: String?,
    detectionResult: SpyDetectionResult
  ): SpyEvent {
    val companyName =
      detectionResult.companyName ?: context.getString(R.string.label_company_unknown)
    val reason =
      detectionResult.reason ?: context.getString(R.string.detection_reason_signature_match)

    return SpyEvent(
      timestamp = System.currentTimeMillis(),
      deviceAddress = result.device.address,
      deviceName = deviceName,
      rssi = result.rssi,
      companyId = companyId?.let { COMPANY_ID_FORMAT.format(it) },
      companyName = companyName,
      manufacturerData = manufacturerDataHex,
      detectionReason = reason,
      matchedDevices = detectionResult.matchedDevices,
      deviceType = detectionResult.deviceType
    )
  }

  companion object {
    private const val TAG = "SpyBleScanner"
    private const val HEX_FORMAT = "%02X"
    private const val COMPANY_ID_FORMAT = "0x%04X"
    private const val SCAN_REPORT_DELAY_MS = 3000L
  }
}
