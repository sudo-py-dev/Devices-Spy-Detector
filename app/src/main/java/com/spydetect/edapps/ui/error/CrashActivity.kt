package com.spydetect.edapps.ui.error

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.spydetect.edapps.R
import com.spydetect.edapps.databinding.ActivityCrashBinding
import com.spydetect.edapps.ui.main.MainActivity

class CrashActivity : AppCompatActivity() {

  private var _binding: ActivityCrashBinding? = null
  private val binding
    get() = checkNotNull(_binding)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityCrashBinding.inflate(layoutInflater)
    setContentView(binding.root)

    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(binding.appbar) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      binding.appbar.setPadding(bars.left, bars.top, bars.right, 0)
      binding.contentScroll.setPadding(
        bars.left,
        binding.contentScroll.paddingTop,
        bars.right,
        bars.bottom
      )
      insets
    }

    val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: ""

    binding.textStackTrace.text = stackTrace

    binding.buttonRestart.setOnClickListener {
      val intent =
        Intent(this, MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      startActivity(intent)
      finish()
    }

    binding.buttonCopyLog.setOnClickListener {
      val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(getString(R.string.clipboard_label_log), stackTrace)
      clipboard.setPrimaryClip(clip)
      Snackbar.make(binding.root, R.string.error_log_copied, Snackbar.LENGTH_SHORT).show()
    }

    binding.textDetailsToggle.setOnClickListener {
      val isVisible = binding.cardStackTrace.visibility == View.VISIBLE
      binding.cardStackTrace.visibility = if (isVisible) View.GONE else View.VISIBLE
      binding.textDetailsToggle.setCompoundDrawablesWithIntrinsicBounds(
        0,
        0,
        if (isVisible) R.drawable.ic_arrow_drop_down_24 else R.drawable.ic_arrow_drop_up_24,
        0
      )
    }
  }

  companion object {
    const val EXTRA_EXCEPTION_MESSAGE = "extra_exception_message"
    const val EXTRA_STACK_TRACE = "extra_stack_trace"
  }
}
