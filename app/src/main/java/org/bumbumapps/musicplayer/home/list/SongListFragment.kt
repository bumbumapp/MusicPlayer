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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.bumbumapps.musicplayer.Globals
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.Timers
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
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = SongsAdapter(
            doOnClick = { song ->
                if (Globals.TIMER_FINISHED){
                    if (mInterstitialAd != null) {
                        mInterstitialAd!!.show(requireActivity())
                        mInterstitialAd!!.fullScreenContentCallback = object :
                            FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                playbackModel.playSong(song)
                                Globals.TIMER_FINISHED = false
                                Timers.timer().start()
                                setUpInterstitialAd()
                                Log.d("TAG", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when fullscreen content failed to show.
                                Log.d("TAG", "The ad failed to show.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                mInterstitialAd = null
                                Log.d("TAG", "The ad was shown.")
                            }
                        }
                    } else {
                        playbackModel.playSong(song)
                    }
                } else {
                    playbackModel.playSong(song)
                }

            },
            ::newMenu
        )

        setupRecycler(R.id.home_song_list, adapter, homeModel.songs)
        setUpInterstitialAd()
        return binding.root
    }
    private fun setUpInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        if (context != null) {
            InterstitialAd.load(
                context, "ca-app-pub-8444865753152507/4732066868", adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd
                        Log.i("TAG", "onAdLoaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.message)
                        mInterstitialAd = null
                    }
                }
            )
        }
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
            holder.bind(data[position])
//            if (!settingsManager.addAudios) {
//                if (!data[position].albumName.contains("Audio")) {
//                    holder.bind(data[position])
//                }
//            } else {
//                holder.bind(data[position])
//            }
        }
    }
}
