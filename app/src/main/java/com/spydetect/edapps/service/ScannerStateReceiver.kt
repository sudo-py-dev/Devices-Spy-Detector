package com.spydetect.edapps.service

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScannerStateReceiver(private val onBluetoothDisabled: () -> Unit) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
      val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
      when (state) {
        BluetoothAdapter.STATE_OFF,
        BluetoothAdapter.STATE_TURNING_OFF -> {
          Log.d("ScannerStateReceiver", "Bluetooth disabled")
          onBluetoothDisabled()
        }
      }
    }
  }
}
