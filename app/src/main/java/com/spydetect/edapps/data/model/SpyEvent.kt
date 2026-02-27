package com.spydetect.edapps.data.model

import android.content.Context
import android.os.Parcelable
import com.spydetect.edapps.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Parcelize
data class SpyEvent(
  val id: Long = 0,
  val timestamp: Long,
  val deviceAddress: String,
  val deviceName: String?,
  val rssi: Int,
  val companyId: String?,
  val companyName: String,
  val manufacturerData: String?,
  val detectionReason: String,
  val matchedDevices: String? = null,
  val deviceType: String? = null,
  val hitCount: Int = 1
) : Parcelable {

  @Transient
  @IgnoredOnParcel
  private val dateFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault()).withZone(ZoneId.systemDefault())

  val timestampFormatted: String
    get() = dateFormatter.format(Instant.ofEpochMilli(timestamp))

  fun toLogString(context: Context): String {
    val name = deviceName ?: context.getString(R.string.label_unknown_device)
    val sb = StringBuilder()
    sb
      .append("Device: ")
      .append(name)
      .append("\nTime: ")
      .append(timestampFormatted)
      .append("\nSignal: ")
      .append(context.getString(R.string.label_rssi_unit, rssi))
      .append("\nReason: ")
      .append(detectionReason)

    if (
      !companyName.isNullOrBlank() &&
        companyName != context.getString(R.string.company_unknown_plain)
    ) {
      sb.append("\nMaker: ").append(companyName)
    }
    if (!deviceType.isNullOrBlank()) {
      sb.append("\nType: ").append(deviceType)
    }

    if (hitCount > 1) sb.append("\nDetected ").append(hitCount).append(" times.")
    return sb.toString()
  }
}
