/*
 * Copyright (c) 2021 Auxio Project
 * SearchAdapter.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.search

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.bumbumapps.musicplayer.music.Album
import org.bumbumapps.musicplayer.music.Artist
import org.bumbumapps.musicplayer.music.BaseModel
import org.bumbumapps.musicplayer.music.Genre
import org.bumbumapps.musicplayer.music.Header
import org.bumbumapps.musicplayer.music.Music
import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.ui.AlbumViewHolder
import org.bumbumapps.musicplayer.ui.ArtistViewHolder
import org.bumbumapps.musicplayer.ui.DiffCallback
import org.bumbumapps.musicplayer.ui.GenreViewHolder
import org.bumbumapps.musicplayer.ui.HeaderViewHolder
import org.bumbumapps.musicplayer.ui.SongViewHolder

/**
 * A Multi-ViewHolder adapter that displays the results of a search query.
 * @author OxygenCobalt
 */
class SearchAdapter(
    private val doOnClick: (data: Music) -> Unit,
    private val doOnLongClick: (view: View, data: Music) -> Unit
) : ListAdapter<BaseModel, RecyclerView.ViewHolder>(DiffCallback<BaseModel>()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Genre -> GenreViewHolder.ITEM_TYPE
            is Artist -> ArtistViewHolder.ITEM_TYPE
            is Album -> AlbumViewHolder.ITEM_TYPE
            is Song -> SongViewHolder.ITEM_TYPE
            is Header -> HeaderViewHolder.ITEM_TYPE

            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GenreViewHolder.ITEM_TYPE -> GenreViewHolder.from(
                parent.context, doOnClick, doOnLongClick
            )

            ArtistViewHolder.ITEM_TYPE -> ArtistViewHolder.from(
                parent.context, doOnClick, doOnLongClick
            )

            AlbumViewHolder.ITEM_TYPE -> AlbumViewHolder.from(
                parent.context, doOnClick, doOnLongClick
            )

            SongViewHolder.ITEM_TYPE -> SongViewHolder.from(
                parent.context, doOnClick, doOnLongClick
            )

            HeaderViewHolder.ITEM_TYPE -> HeaderViewHolder.from(parent.context)

            else -> error("Invalid viewholder item type.")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Genre -> (holder as GenreViewHolder).bind(item)
            is Artist -> (holder as ArtistViewHolder).bind(item)
            is Album -> (holder as AlbumViewHolder).bind(item)
            is Song -> (holder as SongViewHolder).bind(item)
            is Header -> (holder as HeaderViewHolder).bind(item)
            else -> {}
        }
    }
}
