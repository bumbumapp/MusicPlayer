/*
 * Copyright (c) 2021 Auxio Project
 * PlaybackFragment.kt is part of Auxio.
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

package org.bumbumapps.musicplayer.playback

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.bumbumapps.musicplayer.Globals
import org.bumbumapps.musicplayer.Globals.TIMER_FINISHED
import org.bumbumapps.musicplayer.MainFragmentDirections
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.Timers
import org.bumbumapps.musicplayer.databinding.FragmentPlaybackBinding
import org.bumbumapps.musicplayer.detail.DetailViewModel
import org.bumbumapps.musicplayer.playback.state.LoopMode
import org.bumbumapps.musicplayer.ui.memberBinding
import org.bumbumapps.musicplayer.util.logD
import org.bumbumapps.musicplayer.util.systemBarsCompat
import java.util.concurrent.Executors

/**
 * A [Fragment] that displays more information about the song, along with more media controls.
 * Instantiation is done by the navigation component, **do not instantiate this fragment manually.**
 * @author OxygenCobalt
 */
class PlaybackFragment : Fragment() {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    private var mInterstitialAd: InterstitialAd? = null
    private final var TAG = "TAG"
    private val binding by memberBinding(FragmentPlaybackBinding::inflate) {
        playbackSong.isSelected = false // Clear marquee to prevent a memory leak
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val queueItem: MenuItem

        // --- UI SETUP ---

        binding.lifecycleOwner = viewLifecycleOwner
        binding.playbackModel = playbackModel
        binding.detailModel = detailModel
        setUpInterstitialAd()

        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.systemBarsCompat

            binding.root.updatePadding(
                top = bars.top,
                bottom = bars.bottom
            )

            insets
        }

        binding.playbackToolbar.apply {
            setNavigationOnClickListener {
                navigateUp()
            }
            binding.playbackPlayPause.setOnClickListener {
                if (TIMER_FINISHED){
                    if (mInterstitialAd != null) {
                        mInterstitialAd!!.show(requireActivity())
                        mInterstitialAd!!.fullScreenContentCallback = object :
                            FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                playbackModel.invertPlayingStatus()
                                TIMER_FINISHED = false
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
                        playbackModel.invertPlayingStatus()
                    }
                } else {
                    playbackModel.invertPlayingStatus()
                }

            }
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_queue) {
                    findNavController().navigate(MainFragmentDirections.actionShowQueue())

                    true
                } else {
                    false
                }
            }

            queueItem = menu.findItem(R.id.action_queue)
        }

        // Make marquee of song title work
        binding.playbackSong.isSelected = true
        binding.playbackSeekBar.onConfirmListener = playbackModel::setPosition

        binding.playbackPlayPause.post {
            binding.playbackPlayPause.stateListAnimator = null
        }

        // --- VIEWMODEL SETUP --

        playbackModel.song.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                logD("Updating song display to ${song.name}.")

                binding.song = song
                binding.playbackSeekBar.setDuration(song.seconds)
            } else {
                logD("No song is being played, leaving.")

                findNavController().navigateUp()
            }
        }

        playbackModel.isShuffling.observe(viewLifecycleOwner) { isShuffling ->
            binding.playbackShuffle.isActivated = isShuffling
        }

        playbackModel.loopMode.observe(viewLifecycleOwner) { loopMode ->
            val resId = when (loopMode) {
                LoopMode.NONE, null -> R.drawable.ic_loop
                LoopMode.ALL -> R.drawable.ic_loop_on
                LoopMode.TRACK -> R.drawable.ic_loop_one
            }

            binding.playbackLoop.setImageResource(resId)
        }

        playbackModel.position.observe(viewLifecycleOwner) { pos ->
            binding.playbackSeekBar.setProgress(pos)
        }

        playbackModel.nextUp.observe(viewLifecycleOwner) {
            // The queue icon uses a selector that will automatically tint the icon as active or
            // inactive. We just need to set the flag.
            queueItem.isEnabled = playbackModel.nextUp.value!!.isNotEmpty()
        }

        playbackModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.playbackPlayPause.isActivated = isPlaying
        }

        detailModel.navToItem.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                navigateUp()
            }
        }

        logD("Fragment Created.")

        return binding.root
    }

    override fun onDestroyView() {

        super.onDestroyView()
    }

    private fun navigateUp() {
        // This is a dumb and fragile hack but this fragment isn't part of the navigation stack
        // so we can't really do much
        (requireView().parent.parent.parent as PlaybackLayout).collapse()
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


}
