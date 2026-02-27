package com.spydetect.edapps.scanner.protocol

import android.content.Context

data class SpyDetectionResult(
  val isDetected: Boolean,
  val reason: String? = null,
  val companyName: String? = null,
  val matchedDevices: String? = null,
  val deviceType: String? = null
)

interface SpyProtocol {

  fun identify(context: Context, companyId: Int?, deviceName: String?): SpyDetectionResult

  fun getCompanyName(context: Context, companyId: Int): String
}
