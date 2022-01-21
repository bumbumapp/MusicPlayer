/*
 * Copyright (c) 2021 Auxio Project
 * AlbumListFragment.kt is part of Auxio.
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
import androidx.navigation.fragment.findNavController
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.home.HomeFragmentDirections
import org.bumbumapps.musicplayer.music.Genre
import org.bumbumapps.musicplayer.ui.GenreViewHolder
import org.bumbumapps.musicplayer.ui.newMenu
import org.bumbumapps.musicplayer.ui.sliceArticle

/**
 * A [HomeListFragment] for showing a list of [Genre]s.
 * @author
 */
class GenreListFragment : HomeListFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = GenreAdapter(
            doOnClick = { Genre ->
                findNavController().navigate(
                    HomeFragmentDirections.actionShowGenre(Genre.id)
                )
            },
            ::newMenu
        )

        setupRecycler(R.id.home_genre_list, adapter, homeModel.genres)

        return binding.root
    }

    override val listPopupProvider: (Int) -> String
        get() = { idx ->
            homeModel.genres.value!![idx].resolvedName
                .sliceArticle().first().uppercase()
        }

    class GenreAdapter(
        private val doOnClick: (data: Genre) -> Unit,
        private val doOnLongClick: (view: View, data: Genre) -> Unit,
    ) : HomeAdapter<Genre, GenreViewHolder>() {
        override fun getItemCount(): Int = data.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
            return GenreViewHolder.from(parent.context, doOnClick, doOnLongClick)
        }

        override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
            holder.bind(data[position])
        }
    }
}
