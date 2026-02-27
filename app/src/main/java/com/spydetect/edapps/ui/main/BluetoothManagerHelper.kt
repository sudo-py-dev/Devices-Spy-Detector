package com.spydetect.edapps.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.spydetect.edapps.R
import com.spydetect.edapps.util.PermissionManager

class BluetoothManagerHelper(
  private val context: Context,
  private val bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>,
  private val enableBluetoothLauncher: ActivityResultLauncher<Intent>,
  private val onReadyToScan: () -> Unit,
  private val showSnackbar: (String) -> Unit
) {

  fun requestPermissionsAndScan() {
    val missing = PermissionManager.getMissingPermissions(context as android.app.Activity)

    if (missing.isEmpty()) {
      checkBluetoothEnabled()
    } else {
      bluetoothPermissionLauncher.launch(missing.toTypedArray())
    }
  }

  fun checkBluetoothEnabled() {
    val bluetoothManager =
      context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter ?: return
    if (!bluetoothAdapter.isEnabled) {
      enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    } else {
      onReadyToScan()
    }
  }

  fun handleBluetoothResult(resultCode: Int) {
    if (resultCode == android.app.Activity.RESULT_OK) {
      onReadyToScan()
    } else {
      showSnackbar(context.getString(R.string.toast_bluetooth_required))
    }
  }
}
