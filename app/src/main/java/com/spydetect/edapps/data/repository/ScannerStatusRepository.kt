package com.spydetect.edapps.data.repository

import com.spydetect.edapps.data.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScannerStatusRepository {

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _discoveredIds = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
  val discoveredIds: StateFlow<Set<DiscoveredDevice>> = _discoveredIds.asStateFlow()

  fun setScanning(scanning: Boolean) {
    _isScanning.value = scanning
  }

  fun addDiscoveredId(id: Int, rssi: Int, companyName: String? = null) {
    _discoveredIds.update { current ->
      val existing = current.find { it.id == id }
      if (existing != null) {

        current - existing + existing.copy(rssi = rssi, timestamp = System.currentTimeMillis())
      } else {
        current +
          DiscoveredDevice(
            id = id,
            rssi = rssi,
            timestamp = System.currentTimeMillis(),
            companyName = companyName
          )
      }
    }
  }

  fun clearDiscoveredIds() {
    _discoveredIds.value = emptySet()
  }
}
