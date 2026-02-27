package com.spydetect.edapps.ui.main

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.spydetect.edapps.R
import com.spydetect.edapps.util.PermissionManager

class BluetoothManagerHelper(
  private val activity: Activity,
  private val bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>,
  private val enableBluetoothLauncher: ActivityResultLauncher<Intent>,
  private val onReadyToScan: () -> Unit,
  private val showSnackbar: (String) -> Unit
) {

  fun requestPermissionsAndScan() {
    val missing = PermissionManager.getMissingPermissions(activity)

    if (missing.isEmpty()) {
      checkBluetoothEnabled()
    } else {
      bluetoothPermissionLauncher.launch(missing.toTypedArray())
    }
  }

  fun checkBluetoothEnabled() {
    try {
      val bluetoothManager =
        activity.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
      val bluetoothAdapter = bluetoothManager.adapter ?: run {
        showSnackbar(activity.getString(R.string.toast_bluetooth_not_available))
        return
      }
      
      if (!bluetoothAdapter.isEnabled) {
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
      } else {
        onReadyToScan()
      }
    } catch (e: Exception) {
      showSnackbar("Error: Bluetooth not available - ${e.message}")
    }
  }

  fun handleBluetoothResult(resultCode: Int) {
    if (resultCode == Activity.RESULT_OK) {
      onReadyToScan()
    } else {
      showSnackbar(activity.getString(R.string.toast_bluetooth_required))
    }
  }
}
