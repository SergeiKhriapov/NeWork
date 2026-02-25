package ru.netology.nework.ui.dialog

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import ru.netology.nework.databinding.DialogMediaPlayerBinding

private const val TAG = "MediaPlayerDialog"

class MediaPlayerDialog : DialogFragment() {

    private var _binding: DialogMediaPlayerBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = DialogMediaPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        val url = arguments?.getString("mediaUrl")
        val isVideo = arguments?.getBoolean("isVideo", true) ?: true

        if (url.isNullOrBlank()) {
            Log.e(TAG, "URL is null or empty")
            Toast.makeText(requireContext(), "Ошибка: URL не найден", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        Log.d(TAG, "URL: $url, isVideo: $isVideo")

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Показываем заглушку только для аудио
        binding.ivAudioPlaceholder.visibility = if (isVideo) View.GONE else View.VISIBLE

        initializePlayer(url)
    }

    private fun initializePlayer(mediaUrl: String) {
        Log.d(TAG, "initializePlayer: $mediaUrl")
        try {
            player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
                binding.playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                exoPlayer.setMediaItem(mediaItem)

                // Обработка ошибок плеера
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                        Toast.makeText(requireContext(), "Ошибка воспроизведения: ${error.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Playback state changed: $playbackState")
                    }
                })

                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                Log.d(TAG, "Player prepared and started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while creating player", e)
            Toast.makeText(requireContext(), "Не удалось создать плеер: ${e.message}", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }

    companion object {
        fun newInstance(url: String, isVideo: Boolean): MediaPlayerDialog {
            Log.d(TAG, "newInstance: url=$url, isVideo=$isVideo")
            val args = Bundle().apply {
                putString("mediaUrl", url)
                putBoolean("isVideo", isVideo)
            }
            return MediaPlayerDialog().apply { arguments = args }
        }
    }
}