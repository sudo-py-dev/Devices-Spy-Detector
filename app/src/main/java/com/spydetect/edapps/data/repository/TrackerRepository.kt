package com.spydetect.edapps.data.repository

import android.content.Context
import android.util.Log
import com.spydetect.edapps.data.local.dao.TrackerDao
import com.spydetect.edapps.data.local.entity.TrackerEntity
import com.spydetect.edapps.data.local.entity.TrackerEntity.Companion.flatten
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class TrackerRepository(
  private val context: Context,
  private val trackerDao: TrackerDao,
  private val json: kotlinx.serialization.json.Json
) {
  val allTrackers: Flow<List<TrackerEntity>> = trackerDao.getAllTrackers()
  val enabledTrackers: Flow<List<TrackerEntity>> = trackerDao.getEnabledTrackers()
  val availableTypes: Flow<List<String>> = trackerDao.getAvailableTypes()

  @Suppress("TooGenericExceptionCaught")
  suspend fun seedIfNeeded() {
    if (trackerDao.getAllTrackers().first().isEmpty()) {
      Log.d(TAG, "Seeding trackers from local JSON")
      try {
        val jsonString = context.assets.open("trackers.json").bufferedReader().use { it.readText() }
        val trackers = json.decodeFromString<List<TrackerEntity>>(jsonString)

        // Assign types and flatten lists for Room
        val categorized =
          trackers.map {
            it.copy(
              type = categorizeTracker(it),
              devicesFlattened = it.devices.flatten(),
              keywordsFlattened = it.nameKeywords.flatten()
            )
          }

        trackerDao.insertTrackers(categorized)
        Log.i(TAG, "Successfully seeded ${categorized.size} trackers")
      } catch (e: Throwable) {
        Log.e(TAG, "Failed to seed trackers", e)
      }
    }
  }

  suspend fun setTypeEnabled(type: String, enabled: Boolean) {
    trackerDao.setTypeEnabled(type, enabled)
  }

  suspend fun insertTracker(tracker: TrackerEntity) {
    trackerDao.insertTracker(tracker)
  }

  suspend fun deleteTracker(tracker: TrackerEntity) {
    trackerDao.deleteTracker(tracker)
  }

  private fun categorizeTracker(tracker: TrackerEntity): String {
    val commonName = tracker.commonName ?: ""
    val brand = tracker.brand ?: ""
    return when {
      commonName.contains("Apple", ignoreCase = true) ||
        brand.contains("Apple", ignoreCase = true) -> "Apple"
      commonName.contains("Samsung", ignoreCase = true) -> "Samsung"
      commonName.contains("Tile", ignoreCase = true) || brand.contains("Tile", ignoreCase = true) ->
        "Tile"
      commonName.contains("Pebblebee", ignoreCase = true) ||
        brand.contains("Pebblebee", ignoreCase = true) -> "Pebblebee"
      commonName.contains("Chipolo", ignoreCase = true) ||
        brand.contains("Chipolo", ignoreCase = true) -> "Chipolo"
      commonName.contains("Google", ignoreCase = true) -> "Google"
      else -> "Other"
    }
  }

  companion object {
    private const val TAG = "TrackerRepository"
  }
}
