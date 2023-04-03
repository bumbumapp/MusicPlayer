/*
 * Copyright (c) 2021 Auxio Project
 * SettingsListFragment.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.accent.AccentDialog
import org.bumbumapps.musicplayer.excluded.ExcludedDialog
import org.bumbumapps.musicplayer.home.tabs.TabCustomizeDialog
import org.bumbumapps.musicplayer.music.MusicViewModel
import org.bumbumapps.musicplayer.playback.PlaybackViewModel
import org.bumbumapps.musicplayer.settings.pref.IntListPrefDialog
import org.bumbumapps.musicplayer.settings.pref.IntListPreference
import org.bumbumapps.musicplayer.util.*

/**
 * The actual fragment containing the settings menu. Inherits [PreferenceFragmentCompat].
 * @author OxygenCobalt
 */
@Suppress("UNUSED")
class SettingsListFragment : PreferenceFragmentCompat() {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val musicViewModel: MusicViewModel by activityViewModels()
    val settingsManager = SettingsManager.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceScreen.children.forEach { pref ->
            recursivelyHandleChildren(pref)
        }

        preferenceManager.onDisplayPreferenceDialogListener = this

        view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view).apply {
            clipToPadding = false

            setOnApplyWindowInsetsListener { _, insets ->
                updatePadding(bottom = insets.systemBarsCompat.bottom)
                insets
            }
        }

        logD("Fragment created.")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is IntListPreference) {
            IntListPrefDialog.from(preference).show(childFragmentManager, IntListPrefDialog.TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    /**
     * Recursively call [handlePreference] on a preference.
     */
    private fun recursivelyHandleChildren(preference: Preference) {
        if (!preference.isVisible) {
            return
        }

        if (preference is PreferenceCategory) {
            // If this preference is a category of its own, handle its own children
            preference.children.forEach { pref ->
                recursivelyHandleChildren(pref)
            }
        } else {
            handlePreference(preference)
        }
    }

    /**
     * Handle a preference, doing any specific actions on it.
     */
    private fun handlePreference(pref: Preference) {
        pref.apply {
            when (key) {

                SettingsManager.KEY_BLACK_THEME -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        if (requireContext().isNight) {
                            requireActivity().recreate()
                        }

                        true
                    }
                }
                SettingsManager.APP_LICENCE -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        val url="https://github.com/OxygenCobalt/Auxio/blob/dev/LICENSE"
                        val intentUrl = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intentUrl)
                        true

                    }
                }
                SettingsManager.SOURCE_CODE -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        val url="https://github.com/bumbumapp/MusicPlayer"
                        val intentUrl = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intentUrl)
                        true

                    }
                }
                SettingsManager.KEY_ADD_AUDIO -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        var prefence: MyPreference
                        prefence = MyPreference(requireContext())

                        prefence.setBoolen(!prefence.getBoolen())
                        true
                    }
                }
                SettingsManager.KEY_ACCENT -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        AccentDialog().show(childFragmentManager, AccentDialog.TAG)
                        true
                    }

                    summary = context.getString(settingsManager.accent.name)
                }

                SettingsManager.KEY_LIB_TABS -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        TabCustomizeDialog().show(childFragmentManager, TabCustomizeDialog.TAG)
                        true
                    }
                }

                SettingsManager.KEY_SHOW_COVERS, SettingsManager.KEY_QUALITY_COVERS -> {
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        Coil.imageLoader(requireContext()).apply {
                            this.memoryCache?.clear()
                        }

                        true
                    }
                }

                SettingsManager.KEY_SAVE_STATE -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        playbackModel.savePlaybackState(requireContext()) {
                            requireContext().showToast(R.string.lbl_state_saved)
                        }

                        true
                    }
                }

                SettingsManager.KEY_EXCLUDED -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        ExcludedDialog().show(childFragmentManager, ExcludedDialog.TAG)
                        true
                    }
                }
            }
        }
    }

    /**
     * Convert an theme integer into an icon that can be used.
     */
    @DrawableRes
    private fun Int.toThemeIcon(): Int {
        return when (this) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> R.drawable.ic_auto
            AppCompatDelegate.MODE_NIGHT_NO -> R.drawable.ic_day
            AppCompatDelegate.MODE_NIGHT_YES -> R.drawable.ic_night

            else -> R.drawable.ic_auto
        }
    }
}
