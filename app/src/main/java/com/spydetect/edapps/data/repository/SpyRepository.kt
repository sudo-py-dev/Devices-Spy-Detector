package com.spydetect.edapps.data.repository

import com.spydetect.edapps.data.local.dao.SpyDao
import com.spydetect.edapps.data.local.entity.SpyEntity
import com.spydetect.edapps.data.model.SpyEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpyRepository(private val spyDao: SpyDao) {
  private val insertionMutex = Mutex()

  val allDetections: Flow<List<SpyEvent>> =
    spyDao.getAllDetections().map { entities -> entities.map { it.toDomainModel() } }

  suspend fun insertDetection(event: SpyEvent) =
    insertionMutex.withLock {
      val latest = spyDao.getLatestDetectionForDevice(event.deviceAddress, event.companyName)

      if (latest != null) {
        // Merge with existing record
        val updated =
          latest.copy(
            timestamp = event.timestamp,
            rssi = event.rssi,
            detectionReason = event.detectionReason,
            matchedDevices = event.matchedDevices,
            deviceType = event.deviceType,
            hitCount = latest.hitCount + 1
          )
        spyDao.updateDetection(updated)
      } else {
        // New discovery
        spyDao.insertDetection(SpyEntity.fromDomainModel(event))
      }

      spyDao.pruneOldDetections(PRUNE_THRESHOLD)
    }

  suspend fun clearAll() {
    spyDao.deleteAll()
  }

  companion object {
    private const val PRUNE_THRESHOLD = 1000
  }
}
