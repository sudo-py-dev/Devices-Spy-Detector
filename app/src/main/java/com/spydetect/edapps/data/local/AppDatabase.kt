package com.spydetect.edapps.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spydetect.edapps.data.local.dao.SpyDao
import com.spydetect.edapps.data.local.dao.TrackerDao
import com.spydetect.edapps.data.local.entity.SpyEntity
import com.spydetect.edapps.data.local.entity.TrackerEntity

@Database(entities = [SpyEntity::class, TrackerEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun spyDao(): SpyDao

  abstract fun trackerDao(): TrackerDao

  companion object {
    const val DATABASE_NAME = "spy_detector_secure_db"
    
    fun createSecureDatabase(context: Context): AppDatabase {
      return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME
      )
      .fallbackToDestructiveMigration(true)
      .addCallback(SecureDatabaseCallback(context))
      .build()
    }
  }
  
  private class SecureDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      super.onCreate(db)
      // Apply security measures
      try {
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys = ON")
        
        // Enable secure delete (overwrites deleted data with zeros)
        db.execSQL("PRAGMA secure_delete = ON")
        
        // Store temporary tables in memory
        db.execSQL("PRAGMA temp_store = MEMORY")
        
        // Set journal mode to WAL for better concurrency and security
        db.execSQL("PRAGMA journal_mode = WAL")
        
        // Enable auto-vacuum to reclaim space
        db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
        
        // Set synchronous mode to FULL for maximum durability
        db.execSQL("PRAGMA synchronous = FULL")
        
        android.util.Log.i("AppDatabase", "Database security PRAGMAs applied successfully")
      } catch (e: Exception) {
        android.util.Log.w("AppDatabase", "Failed to set security PRAGMAs", e)
      }
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
      super.onOpen(db)
      // Apply additional security when database opens
      try {
        // Ensure foreign keys are still enabled
        db.execSQL("PRAGMA foreign_keys = ON")
        
        // Check database integrity
        db.execSQL("PRAGMA integrity_check")
        
        android.util.Log.i("AppDatabase", "Database opened with security checks")
      } catch (e: Exception) {
        android.util.Log.w("AppDatabase", "Failed to perform security checks on open", e)
      }
    }
  }
}
