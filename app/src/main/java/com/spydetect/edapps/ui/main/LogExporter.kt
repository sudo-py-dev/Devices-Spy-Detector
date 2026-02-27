package com.spydetect.edapps.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spydetect.edapps.R
import com.spydetect.edapps.data.model.SpyEvent
import java.io.File
import java.io.FileWriter

class LogExporter(private val context: Context) {

  fun exportLog(log: List<SpyEvent>, onFileCreated: (File) -> Unit, onError: (String) -> Unit) {
    if (log.isEmpty()) {
      onError(context.getString(R.string.nothing_to_export))
      return
    }
    try {
      val file =
        File(context.getExternalFilesDir(null), "nearby_devices_${System.currentTimeMillis()}.txt")
      FileWriter(file).use { out -> log.forEach { out.write("${it.toLogString(context)}\n") } }
      onFileCreated(file)
    } catch (e: java.io.IOException) {
      onError(e.message ?: context.getString(R.string.error_file_io))
    }
  }

  fun shareFile(file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(
      Intent.createChooser(intent, context.getString(R.string.chooser_export_log))
    )
  }
}
