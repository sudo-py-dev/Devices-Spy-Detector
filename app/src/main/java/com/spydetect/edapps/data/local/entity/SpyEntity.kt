package com.spydetect.edapps.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spydetect.edapps.data.model.SpyEvent

@Entity(tableName = "detections")
data class SpyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long,
  val deviceAddress: String,
  val deviceName: String?,
  val rssi: Int,
  val companyId: String?,
  val companyName: String,
  val manufacturerData: String?,
  val detectionReason: String,
  val matchedDevices: String? = null,
  val deviceType: String? = null,
  val hitCount: Int = 1
) {
  fun toDomainModel() =
    SpyEvent(
      id = id,
      timestamp = timestamp,
      deviceAddress = deviceAddress,
      deviceName = deviceName,
      rssi = rssi,
      companyId = companyId,
      companyName = companyName,
      manufacturerData = manufacturerData,
      detectionReason = detectionReason,
      matchedDevices = matchedDevices,
      deviceType = deviceType,
      hitCount = hitCount
    )

  companion object {
    fun fromDomainModel(event: SpyEvent) =
      SpyEntity(
        id = event.id,
        timestamp = event.timestamp,
        deviceAddress = event.deviceAddress,
        deviceName = event.deviceName,
        rssi = event.rssi,
        companyId = event.companyId,
        companyName = event.companyName,
        manufacturerData = event.manufacturerData,
        detectionReason = event.detectionReason,
        matchedDevices = event.matchedDevices,
        deviceType = event.deviceType,
        hitCount = event.hitCount
      )
  }
}
