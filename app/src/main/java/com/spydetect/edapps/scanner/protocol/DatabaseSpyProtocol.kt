package com.spydetect.edapps.scanner.protocol

import android.content.Context
import com.spydetect.edapps.R
import com.spydetect.edapps.data.local.entity.TrackerEntity
import com.spydetect.edapps.data.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DatabaseSpyProtocol(
  private val repository: TrackerRepository,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SpyProtocol {

  // Cache enabled trackers for performance during scan
  private var cachedTrackers: List<TrackerEntity> = emptyList()

  init {
    scope.launch {
      repository.enabledTrackers.collectLatest { trackers -> cachedTrackers = trackers }
    }
  }

  override fun identify(
    context: Context,
    companyId: Int?,
    deviceName: String?
  ): SpyDetectionResult {
    var result: SpyDetectionResult? = null

    // 1. Try company ID match
    if (companyId != null) {
      val match = cachedTrackers.find { it.companyId == companyId }
      if (match != null) {
        val shouldFlag =
          if (match.keywordList.isNotEmpty() && deviceName != null) {
            val nameLower = deviceName.lowercase()
            match.keywordList.any { keyword -> nameLower.contains(keyword.lowercase()) }
          } else {
            true
          }

        if (shouldFlag) {
          result =
            createDetectionResult(
              context,
              match,
              R.string.detection_reason_company_id_match,
              "0x%04X".format(companyId)
            )
        }
      }
    }

    // 2. Try device name keywords match (only if no company ID match)
    if (result == null && deviceName != null) {
      val nameLower = deviceName.lowercase()
      val match =
        cachedTrackers.find { tracker ->
          tracker.keywordList.any { keyword -> nameLower.contains(keyword.lowercase()) }
        }
      if (match != null) {
        result =
          createDetectionResult(context, match, R.string.detection_reason_name_contains, deviceName)
      }
    }

    return result ?: SpyDetectionResult(isDetected = false)
  }

  private fun createDetectionResult(
    context: Context,
    match: TrackerEntity,
    reasonStringRes: Int,
    reasonValue: String
  ): SpyDetectionResult {
    return SpyDetectionResult(
      isDetected = true,
      reason = context.getString(reasonStringRes, reasonValue),
      companyName = match.displayName,
      matchedDevices = match.deviceList.joinToString(", "),
      deviceType = match.type
    )
  }

  override fun getCompanyName(context: Context, companyId: Int): String {
    return cachedTrackers.find { it.companyId == companyId }?.displayName
      ?: context.getString(R.string.label_company_unknown)
  }
}
