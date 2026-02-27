package com.spydetect.edapps.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spydetect.edapps.R
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.ui.main.MainActivity

class NotificationHelper(private val context: Context) {

  private val notificationManager = NotificationManagerCompat.from(context)

  companion object {
    const val CHANNEL_ID_DETECTION = "spy_alerts"
    const val CHANNEL_ID_SERVICE = "scanner_status"
    const val NOTIFICATION_ID_DETECTION = 1001
    const val NOTIFICATION_ID_SERVICE = 1002
  }

  init {
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.notification_channel_alerts_name)
      val descriptionText = context.getString(R.string.notification_channel_alerts_description)
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val detectionChannel =
        NotificationChannel(CHANNEL_ID_DETECTION, name, importance).apply {
          description = descriptionText
          enableVibration(true)
          setShowBadge(true)
        }
      val serviceName = context.getString(R.string.notification_channel_scanner_name)
      val serviceDescriptionText =
        context.getString(R.string.notification_channel_scanner_description)
      val serviceChannel =
        NotificationChannel(CHANNEL_ID_SERVICE, serviceName, NotificationManager.IMPORTANCE_LOW)
          .apply {
            description = serviceDescriptionText
            setShowBadge(false)
          }

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(detectionChannel)
      manager.createNotificationChannel(serviceChannel)
    }
  }

  fun showDetectionNotification(event: SpyEvent) {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    val deviceName = event.deviceName ?: context.getString(R.string.notification_unknown_device)

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID_DETECTION)
        .setSmallIcon(R.drawable.ic_warning_circle)
        .setContentTitle(context.getString(R.string.notification_title_devices_detected))
        .setContentText(
          context.getString(R.string.notification_detected_text, deviceName, event.rssi)
        )
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(
              context.getString(
                R.string.notification_expanded_text,
                deviceName,
                event.rssi,
                event.detectionReason,
                event.companyName
              )
            )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setColor(
          androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_light_primary)
        )
        .build()

    if (notificationManager.areNotificationsEnabled()) {
      notificationManager.notify(NOTIFICATION_ID_DETECTION, notification)
    }
  }

  fun createServiceNotification(): android.app.Notification {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

    val stopIntent =
      Intent(context, SpyScannerService::class.java).apply {
        action = SpyScannerService.ACTION_STOP_SCAN
      }
    val stopPendingIntent =
      PendingIntent.getService(
        context,
        1,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

    return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
      .setSmallIcon(R.drawable.ic_notification_shield)
      .setContentTitle(context.getString(R.string.notification_service_title))
      .setContentText(context.getString(R.string.notification_service_text))
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .addAction(R.drawable.ic_stop_24, context.getString(R.string.action_stop), stopPendingIntent)
      .build()
  }
}
