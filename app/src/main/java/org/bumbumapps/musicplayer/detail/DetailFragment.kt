/*
 * Copyright (c) 2021 Auxio Project
 * DetailFragment.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.detail

import androidx.annotation.MenuRes
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.databinding.FragmentDetailBinding
import org.bumbumapps.musicplayer.music.MusicParent
import org.bumbumapps.musicplayer.playback.PlaybackViewModel
import org.bumbumapps.musicplayer.ui.MenuSheet
import org.bumbumapps.musicplayer.ui.Sort
import org.bumbumapps.musicplayer.ui.memberBinding
import org.bumbumapps.musicplayer.util.applySpans

/**
 * A Base [Fragment] implementing the base features shared across all detail fragments.
 * @author OxygenCobalt
 */
abstract class DetailFragment : Fragment() {
    protected val detailModel: DetailViewModel by activityViewModels()
    protected val playbackModel: PlaybackViewModel by activityViewModels()
    protected val binding by memberBinding(FragmentDetailBinding::inflate)

    override fun onResume() {
        super.onResume()

        detailModel.setNavigating(false)
    }

    override fun onStop() {
        super.onStop()

        // Cancel all pending menus when this fragment stops to prevent bugs/crashes
        detailModel.finishShowMenu(null)
    }

    /**
     * Shortcut method for doing setup of the detail toolbar.
     * @param data Parent data to use as the toolbar title
     * @param menu Menu resource to use
     * @param onMenuClick (Optional) a click listener for that menu
     */
    protected fun setupToolbar(
        data: MusicParent,
        @MenuRes menu: Int = -1,
        onMenuClick: ((itemId: Int) -> Boolean)? = null
    ) {
        binding.detailToolbar.apply {
            title = data.resolvedName

            if (menu != -1) {
                inflateMenu(menu)
            }

            setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            onMenuClick?.let { onClick ->
                setOnMenuItemClickListener { item ->
                    onClick(item.itemId)
                }
            }
        }
    }

    /**
     * Shortcut method for recyclerview setup
     */
    protected fun setupRecycler(
        detailAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        gridLookup: (Int) -> Boolean
    ) {
        binding.detailRecycler.apply {
            adapter = detailAdapter
            setHasFixedSize(true)

            applySpans(gridLookup)
        }
    }

    /**
     * Shortcut method for spinning up the sorting [MenuSheet]
     * @param config The initial configuration to apply to the menu. This is provided by [DetailViewModel.showMenu].
     * @param showItem Which menu items to keep
     */
    protected fun showMenu(config: DetailViewModel.MenuConfig, showItem: ((Int) -> Boolean)? = null) {
        MenuSheet(config.anchor.context).apply {
            setHeader(getString(R.string.lbl_sort), null)

            val options = MenuSheet.inflateMenu(context, R.menu.menu_detail_sort)
            options.forEach { item ->
                item.isVisible = item.itemId != R.id.option_sort_asc &&
                    (showItem == null || showItem(item.itemId))
            }

            var newMode: Sort? = null

            addItems(options, config.sortMode.itemId) { item ->
                newMode = (newMode ?: config.sortMode).assignId(item.itemId)
            }

            // The ascending toggle stays when the sort direction is supported at all.
            if (showItem == null || showItem(R.id.option_sort_asc)) {
                setDirection(config.sortMode.isAscending) { ascending ->
                    newMode = (newMode ?: config.sortMode).ascending(ascending)
                }
            }

            setOnDismissListener {
                detailModel.finishShowMenu(newMode)
            }

            show()
        }
    }
}
