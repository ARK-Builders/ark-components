package dev.arkbuilders.components.about

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment

class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        val appName = requireArguments().getString(APP_NAME_KEY)
            ?: error("appName can't be null")
        val appLogoResID = requireArguments().getInt(APP_LOGO_KEY)
        val versionName = requireArguments().getString(VERSION_NAME_KEY)
            ?: error("versionName can't be null")
        val privacyPolicyUrl = requireArguments().getString(PRIVACY_POLICY_URL_KEY)
            ?: error("privacyPolicyUrl can't be null")

        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                ArkAbout(
                    appName = appName,
                    appLogoResId = appLogoResID,
                    versionName = versionName,
                    privacyPolicyUrl = privacyPolicyUrl
                )
            }
        }
    }

    companion object {
        private const val APP_NAME_KEY = "APP_NAME_KEY"
        private const val APP_LOGO_KEY = "APP_LOGO_KEY"
        private const val VERSION_NAME_KEY = "VERSION_NAME_KEY"
        private const val PRIVACY_POLICY_URL_KEY = "PRIVACY_POLICY_URL_KEY"

        fun create(
            appName: String,
            @DrawableRes
            appLogoResID: Int,
            versionName: String,
            privacyPolicyUrl: String
        ) = AboutFragment().apply {
            arguments = Bundle().apply {
                putString(APP_NAME_KEY, appName)
                putInt(APP_LOGO_KEY, appLogoResID)
                putString(VERSION_NAME_KEY, versionName)
                putString(PRIVACY_POLICY_URL_KEY, privacyPolicyUrl)
            }
        }
    }
}