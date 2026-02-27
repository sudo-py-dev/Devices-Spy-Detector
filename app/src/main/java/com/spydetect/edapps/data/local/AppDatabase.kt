package com.spydetect.edapps.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spydetect.edapps.data.local.dao.SpyDao
import com.spydetect.edapps.data.local.dao.TrackerDao
import com.spydetect.edapps.data.local.entity.SpyEntity
import com.spydetect.edapps.data.local.entity.TrackerEntity

@Database(entities = [SpyEntity::class, TrackerEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun spyDao(): SpyDao

  abstract fun trackerDao(): TrackerDao

  companion object {
    const val DATABASE_NAME = "spy_detector_db"
  }
}
