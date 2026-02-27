package com.spydetect.edapps
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpyDetectorApp : Application() {
  override fun onCreate() {
    super.onCreate()
    com.spydetect.edapps.util.GlobalExceptionHandler.initialize(this)
  }
}
