/*
 * Copyright (c) 2021 Auxio Project
 * MainActivity.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.bumbumapps.musicplayer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.viewbinding.ViewBinding
import com.google.android.gms.ads.AdView
import org.bumbumapps.musicplayer.accent.Accent
import org.bumbumapps.musicplayer.databinding.ActivityMainBinding
import org.bumbumapps.musicplayer.playback.PlaybackViewModel
import org.bumbumapps.musicplayer.playback.system.PlaybackService
import org.bumbumapps.musicplayer.settings.SettingsManager
import org.bumbumapps.musicplayer.util.isNight
import org.bumbumapps.musicplayer.util.logD
import org.bumbumapps.musicplayer.util.replaceInsetsCompat
import org.bumbumapps.musicplayer.util.systemBarsCompat

/**
 * The single [AppCompatActivity] for Auxio.
 */
class MainActivity : AppCompatActivity() {
    private val playbackModel: PlaybackViewModel by viewModels()
    private lateinit var adview: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupTheme()

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this, R.layout.activity_main
        )
//        loadBannerads(binding)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            applyEdgeToEdgeWindow(binding)
        } else {
            binding.root.fitsSystemWindows = true
        }

        logD("Activity created.")
    }

    override fun onStart() {
        super.onStart()

        startService(Intent(this, PlaybackService::class.java))

        // onNewIntent doesnt automatically call on startup, so call it here.
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // If this intent is a valid view intent that has not been used already, give it
        // to PlaybackViewModel to be used later.
        if (intent != null) {
            val action = intent.action
            val isConsumed = intent.getBooleanExtra(KEY_INTENT_USED, false)

            if (action == Intent.ACTION_VIEW && !isConsumed) {
                // Mark the intent as used so this does not fire again
                intent.putExtra(KEY_INTENT_USED, true)

                intent.data?.let { fileUri ->
                    playbackModel.playWithUri(fileUri, this)
                }
            }
        }
    }

    private fun setupTheme() {
        val settingsManager = SettingsManager.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12, let dynamic colors be our accent and only enable the black theme option
            if (isNight && settingsManager.useBlackTheme) {
                setTheme(R.style.Theme_Auxio_Black)
            }
        } else {
            // Below android 12, load the accent and enable theme customization
            AppCompatDelegate.setDefaultNightMode(settingsManager.theme)
            val newAccent = Accent.set(settingsManager.accent)

            // The black theme has a completely separate set of styles since style attributes cannot
            // be modified at runtime.
            if (isNight && settingsManager.useBlackTheme) {
                setTheme(newAccent.blackTheme)
            } else {
                setTheme(newAccent.theme)
            }
        }
    }

    private fun applyEdgeToEdgeWindow(binding: ViewBinding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Do modern edge to edge, which happens to be around twice the size of the
            // old way of doing things. Thanks android, very cool!
            logD("Doing R+ edge-to-edge.")

            window?.setDecorFitsSystemWindows(false)

            binding.root.setOnApplyWindowInsetsListener { _, insets ->
                WindowInsets.Builder()
                    .setInsets(
                        WindowInsets.Type.systemBars(),
                        insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                    )
                    .build()
                    .applyLeftRightInsets(binding)
            }
        } else {
            // Do old edge-to-edge otherwise.
            logD("Doing legacy edge-to-edge.")

            binding.root.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

                setOnApplyWindowInsetsListener { _, insets ->
                    insets.applyLeftRightInsets(binding)
                }
            }
        }
    }
    private fun loadBannerads(binding: ViewBinding) {
//        MobileAds.initialize(this) {}
//        adview = binding.root.findViewById(R.id.adView)
//
//        binding.root.apply {
//            val adRequest = AdRequest.Builder().build()
//            adview.loadAd(adRequest)
//        }
    }
    private fun WindowInsets.applyLeftRightInsets(binding: ViewBinding): WindowInsets {
        val bars = systemBarsCompat

        binding.root.updatePadding(
            left = bars.left,
            right = bars.right
        )

        return replaceInsetsCompat(0, bars.top, 0, bars.bottom)
    }

    companion object {
        private const val KEY_INTENT_USED = BuildConfig.APPLICATION_ID + ".key.FILE_INTENT_USED"
    }
}
