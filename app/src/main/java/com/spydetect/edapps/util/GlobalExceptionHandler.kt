package com.spydetect.edapps.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spydetect.edapps.R
import com.spydetect.edapps.ui.error.CrashActivity
import kotlin.system.exitProcess

object GlobalExceptionHandler {

  fun initialize(context: Context) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
      try {
        Log.e("GlobalExceptionHandler", "Uncaught exception in thread ${thread.name}", exception)

        val stackTrace =
          java.io
            .StringWriter()
            .also { writer -> exception.printStackTrace(java.io.PrintWriter(writer)) }
            .toString()

        val intent =
          Intent(context, CrashActivity::class.java).apply {
            putExtra(
              CrashActivity.EXTRA_EXCEPTION_MESSAGE,
              exception.message ?: context.getString(R.string.error_unknown)
            )
            putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTrace)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
          }
        context.startActivity(intent)
        exitProcess(1)
      } catch (e: ActivityNotFoundException) {
        Log.e("GlobalExceptionHandler", "Crash activity not found", e)
        defaultHandler?.uncaughtException(thread, exception)
      } catch (e: SecurityException) {
        Log.e("GlobalExceptionHandler", "Security exception in crash handler", e)
        defaultHandler?.uncaughtException(thread, exception)
      }
    }
  }
}
