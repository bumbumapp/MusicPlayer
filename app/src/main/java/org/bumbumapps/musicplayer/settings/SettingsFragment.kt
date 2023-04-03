/*
 * Copyright (c) 2021 Auxio Project
 * SettingsFragment.kt is part of Auxio.
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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jakewharton.processphoenix.ProcessPhoenix
import org.bumbumapps.musicplayer.MainActivity
import org.bumbumapps.musicplayer.databinding.FragmentSettingsBinding
import org.bumbumapps.musicplayer.util.IOBackPressed
import org.bumbumapps.musicplayer.util.MyPreference

/**
 * A container [Fragment] for the settings menu.
 * @author OxygenCobalt
 */
open class SettingsFragment : Fragment(),IOBackPressed  {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater)
        val settingsManager = SettingsManager.getInstance()
        binding.settingsToolbar.apply {

            setNavigationOnClickListener {
                val prefence: MyPreference
                prefence = MyPreference(requireContext())
                if (!prefence.getBoolen()){
                ProcessPhoenix.triggerRebirth(requireContext())
                } else {
                    val back = Intent(requireContext(), MainActivity::class.java)
                    back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(back)
                }
            }
        }

        binding.settingsAppbar.liftOnScrollTargetViewId = androidx.preference.R.id.recycler_view

        return binding.root

    }

    override fun onBackPressed(): Boolean {
       return false
    }

}
