package com.spydetect.edapps.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spydetect.edapps.data.local.entity.TrackerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
  @Query("SELECT * FROM trackers WHERE isEnabled = 1")
  fun getEnabledTrackers(): Flow<List<TrackerEntity>>

  @Query("SELECT * FROM trackers") fun getAllTrackers(): Flow<List<TrackerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackers(trackers: List<TrackerEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTracker(tracker: TrackerEntity)

  @Delete suspend fun deleteTracker(tracker: TrackerEntity)

  @Query("DELETE FROM trackers") suspend fun clearAll()

  @Query("UPDATE trackers SET isEnabled = :enabled WHERE type = :type")
  suspend fun setTypeEnabled(type: String, enabled: Boolean)

  @Query("SELECT DISTINCT type FROM trackers") fun getAvailableTypes(): Flow<List<String>>
}
