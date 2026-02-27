package com.spydetect.edapps.scanner

import kotlinx.coroutines.flow.StateFlow

interface ISpyScanner {
  val isScanning: StateFlow<Boolean>

  fun startScanning(scanMode: Int): Boolean

  fun stopScanning()

  fun updateRssiThreshold(newThreshold: Int)
}
