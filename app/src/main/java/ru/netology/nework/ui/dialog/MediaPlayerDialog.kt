package ru.netology.nework.ui.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import ru.netology.nework.databinding.DialogMediaPlayerBinding

class MediaPlayerDialog : DialogFragment() {

    private var _binding: DialogMediaPlayerBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMediaPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString("mediaUrl")
        val isVideo = arguments?.getBoolean("isVideo", true) ?: true

        if (url.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Error: URL not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        binding.ivAudioPlaceholder.visibility = if (isVideo) View.GONE else View.VISIBLE

        initializePlayer(url)
    }

    private fun initializePlayer(mediaUrl: String) {
        try {
            player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
                binding.playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                exoPlayer.setMediaItem(mediaItem)

                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(requireContext(), "Playback error: ${error.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                    }
                })

                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to create player: ${e.message}", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }

    companion object {
        fun newInstance(url: String, isVideo: Boolean): MediaPlayerDialog {
            val args = Bundle().apply {
                putString("mediaUrl", url)
                putBoolean("isVideo", isVideo)
            }
            return MediaPlayerDialog().apply { arguments = args }
        }
    }
}