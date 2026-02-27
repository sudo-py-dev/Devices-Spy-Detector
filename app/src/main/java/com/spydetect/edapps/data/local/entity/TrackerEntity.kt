package com.spydetect.edapps.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "trackers")
@Serializable
data class TrackerEntity(
  @PrimaryKey(autoGenerate = true) var entryId: Long = 0,
  @SerialName("id") var id: Int? = null,
  @SerialName("hex") var hex: String? = null,
  @SerialName("sig_name") var sigName: String? = null,
  @SerialName("common_name") var commonName: String? = null,
  @SerialName("brand") var brand: String? = null,
  @SerialName("devices") @Transient @Ignore var devices: List<String> = emptyList(),
  @SerialName("name_keywords") @Transient @Ignore var nameKeywords: List<String> = emptyList(),
  @SerialName("verified") var verified: Boolean = false,
  @SerialName("type") var type: String = "Other",
  var isEnabled: Boolean = true,

  // Internal fields for Room to avoid TypeConverters
  var devicesFlattened: String = "",
  var keywordsFlattened: String = ""
) {
  val deviceList: List<String>
    get() = if (devicesFlattened.isEmpty()) devices else devicesFlattened.split(";")

  val keywordList: List<String>
    get() = if (keywordsFlattened.isEmpty()) nameKeywords else keywordsFlattened.split(";")

  val displayName: String?
    get() = commonName ?: brand ?: sigName

  val companyId: Int?
    get() = hex?.removePrefix("0x")?.toIntOrNull(HEX_RADIX)

  companion object {
    private const val HEX_RADIX = 16

    fun List<String>.flatten(): String = joinToString(";")
  }
}
