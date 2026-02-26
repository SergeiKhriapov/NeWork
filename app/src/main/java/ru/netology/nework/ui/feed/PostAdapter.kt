package ru.netology.nework.ui.feed

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import ru.netology.nework.R
import ru.netology.nework.databinding.ItemPostBinding
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "PostAdapter"
private const val PROGRESS_UPDATE_INTERVAL_MS = 200L

class PostAdapter(
    private val onLike: (Post) -> Unit,
    private val onOpen: (Post) -> Unit,
    private val onMenu: (Post, View) -> Unit,
    private val onPlayMedia: (String, Boolean) -> Unit // для видео
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    // Единый плеер для аудио
    private var audioPlayer: ExoPlayer? = null
    private var currentlyPlayingPostId: Long? = null
    private var currentAudioHolder: PostViewHolder? = null

    // Handler для периодического обновления прогресса
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null

    // Обновление UI плеера
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    currentAudioHolder?.updateAudioPlaybackState()
                }
                Player.STATE_ENDED -> {
                    stopAudioPlayback()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            currentAudioHolder?.updateAudioPlaybackState()
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Audio player error: ${error.message}")
            stopAudioPlayback()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder position=$position")
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        releaseAudioPlayer()
    }

    private fun startProgressUpdates() {
        stopProgressUpdates() // на всякий случай
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                currentAudioHolder?.updateAudioPlaybackState()
                mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }.also { mainHandler.post(it) }
    }

    private fun stopProgressUpdates() {
        progressUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        progressUpdateRunnable = null
    }

    private fun stopAudioPlayback() {
        stopProgressUpdates()
        audioPlayer?.stop()
        audioPlayer?.clearMediaItems()
        currentlyPlayingPostId = null
        currentAudioHolder?.updateAudioPlaybackState()
        currentAudioHolder = null
    }

    private fun releaseAudioPlayer() {
        stopProgressUpdates()
        audioPlayer?.removeListener(playerListener)
        audioPlayer?.release()
        audioPlayer = null
        currentlyPlayingPostId = null
        currentAudioHolder = null
    }

    inner class PostViewHolder(private val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {

        private var currentPostId: Long? = null
        private var isAudioPlayingForThisPost = false
            set(value) {
                if (field != value) {
                    field = value
                    updateAudioPlaybackState()
                }
            }

        init {
            // Настройка SeekBar
            b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && audioPlayer != null && currentlyPlayingPostId == currentPostId) {
                        val duration = audioPlayer?.duration ?: 0
                        val seekPosition = ((progress / 100f) * duration).toLong()
                        audioPlayer?.seekTo(seekPosition)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            b.btnPlayPause.setOnClickListener {
                val post = getItem(absoluteAdapterPosition)
                if (post.attachment?.type == AttachmentType.AUDIO) {
                    toggleAudioPlayback(post)
                }
            }
        }

        fun bind(post: Post) = with(b) {
            Log.d(TAG, "bind post id=${post.id}, type=${post.attachment?.type}")
            currentPostId = post.id

            // ---------- AUTHOR ----------
            tvAuthor.text = post.author

            // ---------- AVATAR ----------
            if (!post.authorAvatar.isNullOrBlank()) {
                Glide.with(itemView)
                    .load(post.authorAvatar)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_account_circle)
            }

            // ---------- DATE ----------
            tvDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(post.published))

            // ---------- CONTENT ----------
            if (post.content.isBlank()) tvContent.visibility = View.GONE
            else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            // ---------- LIKES ----------
            tvLikes.text = post.likes.toString()
            ivLike.setImageResource(if (post.likedByMe) R.drawable.ic_liked else R.drawable.ic_like)
            btnLike.setOnClickListener { onLike(post) }

            // ---------- RESET MEDIA ----------
            mediaContainer.visibility = View.GONE
            ivImage.visibility = View.GONE
            ivVideoPreview.visibility = View.GONE
            ivPlay?.visibility = View.GONE
            audioPlayer.visibility = View.GONE
            Glide.with(itemView).clear(ivImage)
            ivVideoPreview.let { Glide.with(itemView).clear(it) }

            // ---------- ATTACHMENT ----------
            val attachment = post.attachment
            if (attachment != null) {
                mediaContainer.visibility = View.VISIBLE

                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        Log.d(TAG, "Post ${post.id}: IMAGE attachment")
                        ivImage.visibility = View.VISIBLE
                        Glide.with(itemView)
                            .load(attachment.url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivImage)
                        ivImage.setOnClickListener { onOpen(post) }
                    }

                    AttachmentType.VIDEO -> {
                        Log.d(TAG, "Post ${post.id}: VIDEO attachment")
                        val videoUrl = attachment.url
                        if (!post.link.isNullOrBlank() && post.link != videoUrl) {
                            ivVideoPreview.visibility = View.VISIBLE
                            ivPlay?.visibility = View.VISIBLE
                            Glide.with(itemView)
                                .load(post.link)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(ivVideoPreview)
                        } else {
                            ivVideoPreview.visibility = View.VISIBLE
                            ivPlay?.visibility = View.VISIBLE
                            Glide.with(itemView)
                                .load(videoUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .frame(1000000)
                                .centerCrop()
                                .placeholder(R.drawable.ic_play_circle_filled)
                                .error(R.drawable.ic_play_circle_filled)
                                .into(ivVideoPreview)
                        }
                        if (!videoUrl.isNullOrBlank()) {
                            ivVideoPreview.setOnClickListener { onPlayMedia(videoUrl, true) }
                            ivPlay?.setOnClickListener { onPlayMedia(videoUrl, true) }
                        }
                    }

                    AttachmentType.AUDIO -> {
                        Log.d(TAG, "Post ${post.id}: AUDIO attachment")
                        audioPlayer.visibility = View.VISIBLE
                        mediaContainer.visibility = View.GONE   // скрываем контейнер, чтобы не было пустого места
                        ivImage.visibility = View.GONE
                        ivVideoPreview.visibility = View.GONE
                        ivPlay?.visibility = View.GONE

                        // Определяем, играет ли сейчас этот пост
                        isAudioPlayingForThisPost = (currentlyPlayingPostId == post.id)

                        if (isAudioPlayingForThisPost) {
                            currentAudioHolder = this@PostViewHolder
                            updateAudioPlaybackState()
                        } else {
                            btnPlayPause.setImageResource(R.drawable.ic_audio_play)
                            tvAudioDuration.text = formatDuration(0) + " / " + formatDuration(0)
                            seekBar.progress = 0
                        }
                    }

                    else -> {
                        Log.w(TAG, "Unknown attachment type: ${attachment.type}")
                        mediaContainer.visibility = View.GONE
                    }
                }
            } else {
                Log.d(TAG, "Post ${post.id}: no attachment")
            }

            // ---------- OTHER CLICKS ----------
            root.setOnClickListener { onOpen(post) }
            btnMore.setOnClickListener { onMenu(post, btnMore) }
        }

        fun toggleAudioPlayback(post: Post) {
            if (currentlyPlayingPostId == post.id) {
                // Останавливаем текущее
                audioPlayer?.pause()
                currentlyPlayingPostId = null
                currentAudioHolder = null
                isAudioPlayingForThisPost = false
                stopProgressUpdates()
            } else {
                // Запускаем новое
                if (audioPlayer == null) {
                    audioPlayer = ExoPlayer.Builder(itemView.context).build().also {
                        it.addListener(playerListener)
                    }
                }
                // Останавливаем предыдущее
                if (currentlyPlayingPostId != null) {
                    currentAudioHolder?.isAudioPlayingForThisPost = false
                    currentAudioHolder = null
                }
                val mediaItem = MediaItem.fromUri(android.net.Uri.parse(post.attachment?.url ?: return))
                audioPlayer?.setMediaItem(mediaItem)
                audioPlayer?.prepare()
                audioPlayer?.play()
                currentlyPlayingPostId = post.id
                currentAudioHolder = this
                isAudioPlayingForThisPost = true
                startProgressUpdates() // запускаем обновление прогресса
            }
        }

        fun updateAudioPlaybackState() {
            if (currentlyPlayingPostId != currentPostId) return
            val player = audioPlayer ?: return
            val isPlaying = player.isPlaying
            b.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_audio_pause_24 else R.drawable.ic_audio_play)
            val current = player.currentPosition
            val total = player.duration
            if (total > 0) {
                b.seekBar.progress = ((current.toFloat() / total) * 100).toInt()
                b.tvAudioDuration.text = formatDuration(current) + " / " + formatDuration(total)
            }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}