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

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.bumbumapps.musicplayer.util.shareSong
import org.bumbumapps.musicplayer.util.showToast
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.bumbumapps.musicplayer.*
import org.bumbumapps.musicplayer.databinding.FragmentPlaybackBinding
import org.bumbumapps.musicplayer.detail.DetailViewModel
import org.bumbumapps.musicplayer.playback.state.LoopMode
import org.bumbumapps.musicplayer.settings.SettingsManager
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
    private final var TAG = "TAG"
    private val settingsManager = SettingsManager.getInstance()
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
                playbackModel.invertPlayingStatus()
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_queue -> {
                        findNavController().navigate(MainFragmentDirections.actionShowQueue())
                        true
                    }
                    R.id.action_sleep_timer -> {
                        showSleepTimerDialog()
                        true
                    }
                    else -> false
                }
            }

            queueItem = menu.findItem(R.id.action_queue)
        }

        binding.playbackShare?.setOnClickListener {
            playbackModel.song.value?.let { requireContext().shareSong(it) }
        }

        // Soft-focus the cover backdrop where the platform can blur views.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.playbackBackdrop?.setRenderEffect(
                RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP)
            )
        }

        SleepTimer.active.observe(viewLifecycleOwner) { active ->
            binding.playbackToolbar.menu.findItem(R.id.action_sleep_timer)
                ?.setIcon(if (active) R.drawable.ic_timer_on else R.drawable.ic_timer)
        }

        // Make marquee of song title work
        binding.playbackSong.isSelected = true
        binding.playbackSeekBar.onConfirmListener = playbackModel::setPosition

        // Capture the view: the binding is gone if the view is destroyed before this runs.
        val playPause = binding.playbackPlayPause
        playPause.post { playPause.stateListAnimator = null }

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

        playbackModel.nextUp.observe(viewLifecycleOwner) {queue->
            // The queue icon uses a selector that will automatically tint the icon as active or
            // inactive. We just need to set the flag.
            if (!settingsManager.addAudios) {
                queue.filter {
                    !it.albumName.contains("Audio") && !it.albumName.isDigitsOnly()
                }.toMutableList()
            }else queue.toMutableList()

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

    private fun showSleepTimerDialog() {
        val ctx = requireContext()
        val minutes = SleepTimer.OPTIONS_MINUTES
        val labels = minutes.map { ctx.getString(R.string.fmt_sleep_minutes, it) }.toMutableList()
        if (SleepTimer.isActive) {
            labels.add(0, ctx.getString(R.string.lbl_sleep_timer_off))
        }
        val title = if (SleepTimer.isActive) {
            ctx.getString(R.string.fmt_sleep_remaining, SleepTimer.format(SleepTimer.remainingMs()))
        } else {
            ctx.getString(R.string.lbl_sleep_timer)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setItems(labels.toTypedArray()) { _, which ->
                val index = if (SleepTimer.isActive) which - 1 else which
                if (index < 0) {
                    SleepTimer.cancel()
                    ctx.showToast(R.string.lbl_sleep_cancelled)
                } else {
                    SleepTimer.start(minutes[index])
                    Toast.makeText(
                        ctx, ctx.getString(R.string.fmt_sleep_set, minutes[index]), Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun navigateUp() {
        // This is a dumb and fragile hack but this fragment isn't part of the navigation stack
        // so we can't really do much
        (requireView().parent.parent.parent as PlaybackLayout).collapse()
    }



}
