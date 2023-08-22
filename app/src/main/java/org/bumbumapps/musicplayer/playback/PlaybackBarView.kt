package org.bumbumapps.musicplayer.playback
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.color.MaterialColors
import org.bumbumapps.musicplayer.AdsLoader
import org.bumbumapps.musicplayer.Globals
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.Timers
import org.bumbumapps.musicplayer.databinding.ViewPlaybackBarBinding
import org.bumbumapps.musicplayer.detail.DetailViewModel
import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.util.inflater
import org.bumbumapps.musicplayer.util.resolveAttr
import org.bumbumapps.musicplayer.util.systemBarsCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A view displaying the playback state in a compact manner. This is only meant to be used
 * by [PlaybackLayout].
 */
class PlaybackBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding = ViewPlaybackBarBinding.inflate(context.inflater, this, true)
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    private final var TAG = "TAG"

    init {
        id = R.id.playback_bar
        // Deliberately override the progress bar color [in a Lollipop-friendly way] so that
        // we use colorSecondary instead of colorSurfaceVariant. This is for two reasons:
        // 1. colorSurfaceVariant is used with the assumption that the view that is using it
        // is not elevated and is therefore not colored. This view is elevated.
        // 2. The way a solid color plays along with a ripple just doesnt look that good.
        binding.playbackProgressBar.trackColor = MaterialColors.compositeARGBWithAlpha(
            R.attr.colorSecondary.resolveAttr(context), (255 * 0.2).toInt()
        )
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        updatePadding(bottom = insets.systemBarsCompat.bottom)

        return insets
    }

    fun setup(
        playbackModel: PlaybackViewModel,
        detailModel: DetailViewModel,
        viewLifecycleOwner: LifecycleOwner
    ) {
        setOnLongClickListener {
            playbackModel.song.value?.let { song ->
                detailModel.navToItem(song)
            }
            true
        }

        binding.playbackSkipPrev?.setOnClickListener {
            playbackModel.skipPrev()
        }

        binding.playbackPlayPause.setOnClickListener {
            AdsLoader.showAds(context){playbackModel.invertPlayingStatus()}

        }

        binding.playbackSkipNext?.setOnClickListener {
            playbackModel.skipNext()
        }

        binding.playbackPlayPause.isActivated = playbackModel.isPlaying.value!!

        playbackModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.playbackPlayPause.isActivated = isPlaying
        }

        binding.playbackProgressBar.progress = playbackModel.position.value!!.toInt()

        playbackModel.position.observe(viewLifecycleOwner) { position ->
            binding.playbackProgressBar.progress = position.toInt()
        }
    }

    fun setSong(song: Song) {
        binding.song = song
        binding.executePendingBindings()
    }



}
