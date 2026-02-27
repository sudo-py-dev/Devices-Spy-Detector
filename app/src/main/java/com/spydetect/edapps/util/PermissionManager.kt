package com.spydetect.edapps.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionManager {

  fun getRequiredPermissions(): List<String> {
    val permissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(Manifest.permission.BLUETOOTH_SCAN)
      permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
      permissions.add(Manifest.permission.BLUETOOTH)
      permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
      permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    return permissions
  }

  fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) ==
      PackageManager.PERMISSION_GRANTED
  }

  fun getMissingPermissions(context: Context): List<String> {
    return getRequiredPermissions().filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
  }

  fun isBatteryOptimizationDisabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
      true
    }
  }

  fun requestDisableBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
          data = Uri.parse("package:${context.packageName}")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      } else {

        val settingsIntent =
          Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
        context.startActivity(settingsIntent)
      }
    }
  }
}
