package com.spydetect.edapps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spydetect.edapps.data.local.entity.SpyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpyDao {
  @Query("SELECT * FROM detections ORDER BY timestamp DESC")
  fun getAllDetections(): Flow<List<SpyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDetection(detection: SpyEntity)

  @Query(
    "SELECT * FROM detections WHERE deviceAddress = :address " +
      "AND companyName = :companyName ORDER BY timestamp DESC LIMIT 1"
  )
  suspend fun getLatestDetectionForDevice(address: String, companyName: String): SpyEntity?

  @androidx.room.Update suspend fun updateDetection(detection: SpyEntity)

  @Query("DELETE FROM detections") suspend fun deleteAll()

  @Query(
    "DELETE FROM detections WHERE id NOT IN (SELECT id FROM detections ORDER BY timestamp DESC LIMIT :limit)"
  )
  suspend fun pruneOldDetections(limit: Int)
}
