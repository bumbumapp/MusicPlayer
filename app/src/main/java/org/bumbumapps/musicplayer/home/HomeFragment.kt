/*
 * Copyright (c) 2021 Auxio Project
 * MainFragment.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import org.bumbumapps.musicplayer.*
import org.bumbumapps.musicplayer.databinding.FragmentHomeBinding
import org.bumbumapps.musicplayer.detail.DetailViewModel
import org.bumbumapps.musicplayer.home.list.AlbumListFragment
import org.bumbumapps.musicplayer.home.list.ArtistListFragment
import org.bumbumapps.musicplayer.home.list.GenreListFragment
import org.bumbumapps.musicplayer.home.list.SongListFragment
import org.bumbumapps.musicplayer.music.Album
import org.bumbumapps.musicplayer.music.Artist
import org.bumbumapps.musicplayer.music.Genre
import org.bumbumapps.musicplayer.music.MusicStore
import org.bumbumapps.musicplayer.music.MusicViewModel
import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.playback.PlaybackViewModel
import org.bumbumapps.musicplayer.ui.DisplayMode
import org.bumbumapps.musicplayer.ui.MenuSheet
import org.bumbumapps.musicplayer.util.logD
import org.bumbumapps.musicplayer.util.logE

/**
 * The main "Launching Point" fragment of Auxio, allowing navigation to the detail
 * views for each respective item.
 * @author OxygenCobalt
 */
class HomeFragment : Fragment() {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    private val homeModel: HomeViewModel by activityViewModels()
    private val musicModel: MusicViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater)

        // --- UI SETUP ---

        binding.lifecycleOwner = viewLifecycleOwner

        binding.homeGreeting.setText(
            when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                in 5..11 -> R.string.lbl_greeting_morning
                in 12..17 -> R.string.lbl_greeting_afternoon
                else -> R.string.lbl_greeting_evening
            }
        )
        binding.homeSearch.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionShowSearch())
        }
        binding.homeSort.setOnClickListener { showSortSheet() }
        binding.homeSettings.setOnClickListener {
            parentFragment?.parentFragment?.findNavController()?.navigate(
                MainFragmentDirections.actionShowSettings1()
            )
        }
        AdsLoader.displayInterstitial(requireContext())

        binding.homePager.apply {
            adapter = HomePagerAdapter()

            // By default, ViewPager2's sensitivity is high enough to result in vertical
            // scroll events being registered as horizontal scroll events. Reflect into the
            // internal recyclerview and change the touch slope so that touch actions will
            // act more as a scroll than as a swipe.
            // Derived from: https://al-e-shevelev.medium.com/how-to-reduce-scroll-sensitivity-of-viewpager2-widget-87797ad02414
            try {
                val recycler = ViewPager2::class.java.getDeclaredField("mRecyclerView").run {
                    isAccessible = true
                    get(binding.homePager)
                }

                RecyclerView::class.java.getDeclaredField("mTouchSlop").apply {
                    isAccessible = true

                    val slop = get(recycler) as Int
                    set(recycler, slop * 3) // 3x seems to be the best fit here
                }
            } catch (e: Exception) {
                logE("Unable to reduce ViewPager sensitivity")
                logE(e.stackTraceToString())
            }

            // We know that there will only be a fixed amount of tabs, so we manually set this
            // limit to that. This also prevents the appbar lift state from being confused during
            // page transitions.
            offscreenPageLimit = homeModel.tabs.size

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = homeModel.updateCurrentTab(position)
            })

            TabLayoutMediator(
                binding.homeTabs, this, AdaptiveTabStrategy(context, homeModel)
            ).attach()
        }

        binding.homeHero.setOnClickListener {
            AdsLoader.showAds(requireContext()) { playbackModel.shuffleAll() }
        }

        // --- VIEWMODEL SETUP ---

        // There is no way a fast scrolling event can continue across a re-create. Reset it.
        homeModel.updateFastScrolling(false)

        musicModel.loaderResponse.observe(viewLifecycleOwner) { response ->
            // Handle the loader response.
            when (response) {
                is MusicStore.Response.Ok -> {
                    val count = MusicStore.maybeGetInstance()?.songs?.size ?: 0
                    binding.homeHeroCount.text =
                        resources.getQuantityString(R.plurals.fmt_song_count, count, count)
                    binding.homeHero.visibility = View.VISIBLE
                }

                // While loading or during an error, keep shuffle hidden so that no playback can
                // start before the library exists. PlaybackStateManager relies on this invariant.
                else -> binding.homeHero.visibility = View.INVISIBLE
            }
        }

        homeModel.recreateTabs.observe(viewLifecycleOwner) { recreate ->
            // notifyDataSetChanged is not practical for recreating here since it will cache
            // the previous fragments. Just instantiate a whole new adapter.
            if (recreate) {
                binding.homePager.currentItem = 0
                binding.homePager.adapter = HomePagerAdapter()
                homeModel.finishRecreateTabs()
            }
        }

        homeModel.curTab.observe(viewLifecycleOwner) { t ->
            val tab = requireNotNull(t)

            // Make sure that we update the scrolling view whenever the tab changes.
            binding.homeAppbar.liftOnScrollTargetViewId = tab.viewId
            binding.homeAppbar.setExpanded(true, false)
        }

        detailModel.navToItem.observe(viewLifecycleOwner) { item ->
            // The AppBarLayout gets confused and collapses when we navigate too fast, wait for it
            // to draw before we continue.
            binding.homeAppbar.post {
                when (item) {
                    is Song -> findNavController().navigate(
                        HomeFragmentDirections.actionShowAlbum(item.album.id)
                    )

                    is Album -> findNavController().navigate(
                        HomeFragmentDirections.actionShowAlbum(item.id)
                    )

                    is Artist -> findNavController().navigate(
                        HomeFragmentDirections.actionShowArtist(item.id)
                    )

                    is Genre -> findNavController().navigate(
                        HomeFragmentDirections.actionShowGenre(item.id)
                    )

                    else -> {
                    }
                }
            }
        }

        logD("Fragment Created.")
        return binding.root
    }

    /**
     * Show the sort options for the current tab in a [MenuSheet].
     */
    private fun showSortSheet() {
        val tab = homeModel.curTab.value ?: return
        val sort = homeModel.getSortForDisplay(tab)

        MenuSheet(requireContext()).apply {
            setHeader(getString(R.string.lbl_sort), null)

            val options = MenuSheet.inflateMenu(requireContext(), R.menu.menu_detail_sort)
            options.forEach { option -> option.isVisible = isSortAllowed(tab, option.itemId) }

            addItems(options, sort.itemId) { option ->
                val new = homeModel.getSortForDisplay(tab).assignId(option.itemId)
                homeModel.updateCurrentSort(requireNotNull(new))
            }

            setDirection(sort.isAscending) { ascending ->
                homeModel.updateCurrentSort(homeModel.getSortForDisplay(tab).ascending(ascending))
            }
        }.show()
    }

    private fun isSortAllowed(displayMode: DisplayMode, id: Int): Boolean = when (displayMode) {
        DisplayMode.SHOW_SONGS -> id != R.id.option_sort_asc
        DisplayMode.SHOW_ALBUMS -> id != R.id.option_sort_asc && id != R.id.option_sort_album
        // Artists and genres sort by name only.
        DisplayMode.SHOW_ARTISTS, DisplayMode.SHOW_GENRES -> id == R.id.option_sort_name
    }

    private val DisplayMode.viewId: Int get() = when (this) {
        DisplayMode.SHOW_SONGS -> R.id.home_song_list
        DisplayMode.SHOW_ALBUMS -> R.id.home_album_list
        DisplayMode.SHOW_ARTISTS -> R.id.home_artist_list
        DisplayMode.SHOW_GENRES -> R.id.home_genre_list
    }

    private inner class HomePagerAdapter :
        FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {

        override fun getItemCount(): Int = homeModel.tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (homeModel.tabs[position]) {
                DisplayMode.SHOW_SONGS -> SongListFragment()
                DisplayMode.SHOW_ALBUMS -> AlbumListFragment()
                DisplayMode.SHOW_ARTISTS -> ArtistListFragment()
                DisplayMode.SHOW_GENRES -> GenreListFragment()
            }
        }
    }


}
