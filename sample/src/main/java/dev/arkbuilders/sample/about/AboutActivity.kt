package dev.arkbuilders.sample.about

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.arkbuilders.components.about.AboutFragment
import dev.arkbuilders.sample.BuildConfig
import dev.arkbuilders.sample.R

class AboutActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val aboutFragment = AboutFragment.create(
            appName = getString(R.string.app_name),
            appLogoResID = R.drawable.ic_launcher_foreground,
            versionName = BuildConfig.VERSION_NAME,
            privacyPolicyUrl = ""
        )

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.about_content, aboutFragment)
            .commit()
    }
}