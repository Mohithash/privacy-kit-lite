package com.sal.privacykit.ui.shortcut

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sal.privacykit.PrivacyKitApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val EXTRA_PROFILE_ID = "profileId"

/**
 * No-UI trampoline target for home-screen shortcuts pinned from [com.sal.privacykit.ui.profiles.ProfileDetailsScreen].
 * Replays the exact same launch sequence as the in-app "Launch" button (see [com.sal.privacykit.data.ProfileLauncher])
 * so a shortcut tap uses the same active-profile export path, just triggered from the home screen.
 */
class ShortcutLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        if (profileId < 0) {
            finish()
            return
        }
        val container = (application as PrivacyKitApplication).container
        lifecycleScope.launch {
            val profile = container.profileRepository.observeById(profileId).first()
            if (profile == null) {
                Toast.makeText(this@ShortcutLaunchActivity, "This profile no longer exists", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            container.profileLauncher.launch(profile, this@ShortcutLaunchActivity)?.let { error ->
                Toast.makeText(this@ShortcutLaunchActivity, error, Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    companion object {
        fun intentFor(context: Context, profileId: Long): Intent =
            Intent(context, ShortcutLaunchActivity::class.java)
                .putExtra(EXTRA_PROFILE_ID, profileId)
                .setAction(Intent.ACTION_VIEW)
    }
}
