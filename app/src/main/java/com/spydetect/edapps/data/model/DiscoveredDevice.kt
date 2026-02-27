package com.spydetect.edapps.data.model

data class DiscoveredDevice(
  val id: Int,
  val rssi: Int,
  val timestamp: Long,
  val companyName: String? = null
)
