package com.spydetect.edapps.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.spydetect.edapps.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
  private lateinit var binding: ActivityAboutBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAboutBinding.inflate(layoutInflater)
    setContentView(binding.root)

    WindowCompat.setDecorFitsSystemWindows(window, false)
    val appBar = binding.appbar
    val toolbar = binding.toolbar
    val content = binding.contentScroll
    ViewCompat.setOnApplyWindowInsetsListener(appBar) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      appBar.setPadding(bars.left, bars.top, bars.right, 0)
      content.setPadding(bars.left, content.paddingTop, bars.right, bars.bottom)
      insets
    }

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setDisplayShowHomeEnabled(true)

    binding.cardPrivacy.setOnClickListener {
      startActivity(Intent(this, PrivacyActivity::class.java))
    }

    binding.cardLicenses.setOnClickListener {
      startActivity(Intent(this, LicensesActivity::class.java))
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
  }

  fun openGithub(view: android.view.View) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sudo-py-dev/Devices-Spy-Detector"))
        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    startActivity(intent)
  }
}
