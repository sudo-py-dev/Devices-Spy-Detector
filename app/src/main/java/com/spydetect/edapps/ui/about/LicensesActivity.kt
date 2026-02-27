package com.spydetect.edapps.ui.about

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import com.spydetect.edapps.databinding.ActivityLicensesBinding

class LicensesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.licensesContent.text = Html.fromHtml(
            getString(com.spydetect.edapps.R.string.licenses_content),
            Html.FROM_HTML_MODE_COMPACT
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
