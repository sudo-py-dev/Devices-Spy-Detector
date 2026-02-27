package com.spydetect.edapps.di

import android.content.Context
import androidx.room.Room
import com.spydetect.edapps.data.local.AppDatabase
import com.spydetect.edapps.data.local.dao.SpyDao
import com.spydetect.edapps.data.local.dao.TrackerDao
import com.spydetect.edapps.data.repository.PreferenceRepository
import com.spydetect.edapps.data.repository.ScannerStatusRepository
import com.spydetect.edapps.data.repository.SpyRepository
import com.spydetect.edapps.data.repository.TrackerRepository
import com.spydetect.edapps.scanner.ISpyScanner
import com.spydetect.edapps.scanner.SpyBleScanner
import com.spydetect.edapps.scanner.protocol.DatabaseSpyProtocol
import com.spydetect.edapps.scanner.protocol.SpyProtocol
import com.spydetect.edapps.service.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun provideSpyDao(db: AppDatabase): SpyDao = db.spyDao()

    @Provides
    @Singleton
    fun provideTrackerDao(db: AppDatabase): TrackerDao = db.trackerDao()

    @Provides
    @Singleton
    fun providePreferenceRepository(@ApplicationContext context: Context): PreferenceRepository {
        return PreferenceRepository(context)
    }

    @Provides
    @Singleton
    fun provideSpyRepository(spyDao: SpyDao): SpyRepository = SpyRepository(spyDao)

    @Provides
    @Singleton
    fun provideScannerStatusRepository(): ScannerStatusRepository = ScannerStatusRepository()

    @Provides
    @Singleton
    fun provideTrackerRepository(
        @ApplicationContext context: Context,
        trackerDao: TrackerDao,
        json: Json
    ): TrackerRepository = TrackerRepository(context, trackerDao, json)

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideSpyProtocols(trackerRepository: TrackerRepository): List<@JvmSuppressWildcards SpyProtocol> {
        return listOf(DatabaseSpyProtocol(trackerRepository))
    }
}

interface ScannerFactory {
    fun create(rssi: Int, onDetected: (com.spydetect.edapps.data.model.SpyEvent) -> Unit): ISpyScanner
}

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides
    @Singleton
    fun provideScannerFactory(
        @ApplicationContext context: Context,
        protocols: List<@JvmSuppressWildcards SpyProtocol>,
        scanStatus: ScannerStatusRepository
    ): ScannerFactory = object : ScannerFactory {
        override fun create(rssi: Int, onDetected: (com.spydetect.edapps.data.model.SpyEvent) -> Unit): ISpyScanner {
            return SpyBleScanner(
                context = context,
                rssiThreshold = rssi,
                protocols = protocols,
                onCompanyIdDiscovered = { id, rssiVal, companyName ->
                    scanStatus.addDiscoveredId(id, rssiVal, companyName)
                },
                onDeviceDetected = onDetected
            )
        }
    }
}
