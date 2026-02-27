package com.spydetect.edapps.ui.about

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import com.spydetect.edapps.databinding.ActivityPrivacyBinding

class PrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.privacyContent.text = Html.fromHtml(
            getString(com.spydetect.edapps.R.string.privacy_policy_content),
            Html.FROM_HTML_MODE_COMPACT
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
