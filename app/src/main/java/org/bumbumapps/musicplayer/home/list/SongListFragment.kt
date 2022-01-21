/*
 * Copyright (c) 2021 Auxio Project
 * SongListFragment.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.home.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.settings.SettingsManager
import org.bumbumapps.musicplayer.ui.DisplayMode
import org.bumbumapps.musicplayer.ui.SongViewHolder
import org.bumbumapps.musicplayer.ui.Sort
import org.bumbumapps.musicplayer.ui.newMenu
import org.bumbumapps.musicplayer.ui.sliceArticle

/**
 * A [HomeListFragment] for showing a list of [Song]s.
 * @author
 */
class SongListFragment : HomeListFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = SongsAdapter(
            doOnClick = { song ->
                playbackModel.playSong(song)
            },
            ::newMenu
        )

        setupRecycler(R.id.home_song_list, adapter, homeModel.songs)

        return binding.root
    }

    override val listPopupProvider: (Int) -> String
        get() = { idx ->
            val song = homeModel.songs.value!![idx]

            // Change how we display the popup depending on the mode.
            when (homeModel.getSortForDisplay(DisplayMode.SHOW_SONGS)) {
                // Name -> Use name
                is Sort.ByName -> song.name.sliceArticle()
                    .first().uppercase()

                // Artist -> Use Artist Name
                is Sort.ByArtist ->
                    song.album.artist.resolvedName
                        .sliceArticle().first().uppercase()

                // Album -> Use Album Name
                is Sort.ByAlbum -> song.album.name.sliceArticle()
                    .first().uppercase()

                // Year -> Use Full Year
                is Sort.ByYear -> song.album.year.toString()
            }
        }

    inner class SongsAdapter(
        private val doOnClick: (data: Song) -> Unit,
        private val doOnLongClick: (view: View, data: Song) -> Unit,
    ) : HomeAdapter<Song, SongViewHolder>() {
        override fun getItemCount(): Int = data.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            return SongViewHolder.from(parent.context, doOnClick, doOnLongClick)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            val settingsManager = SettingsManager.getInstance()
            if (!settingsManager.addAudios) {
                if (!data[position].albumName.contains("Audio")) {
                    holder.bind(data[position])
                }
            } else {
                holder.bind(data[position])
            }
        }
    }
}
