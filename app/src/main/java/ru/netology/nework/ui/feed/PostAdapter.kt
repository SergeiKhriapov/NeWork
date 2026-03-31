package ru.netology.nework.ui.feed

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.SeekBar
import androidx.core.content.ContextCompat
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
import ru.netology.nework.utils.DateUtils.formatForDisplay
import ru.netology.nework.utils.LetterAvatarDrawable

private const val TAG = "PostAdapter"
private const val PROGRESS_UPDATE_INTERVAL_MS = 500L // Увеличил интервал с 200ms до 500ms
private const val SEEK_BAR_UPDATE_THRESHOLD = 2 // Обновляем только при изменении прогресса >= 2%

class PostAdapter(
    private val onLike: (Post) -> Unit,
    private val onOpen: (Post) -> Unit,
    private val onMenu: (Post, View) -> Unit,
    private val onPlayMedia: (String, Boolean) -> Unit,
    private val onShare: (Post) -> Unit,
    private val isOwnedByUser: (Post) -> Boolean
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    private var audioPlayer: ExoPlayer? = null
    private var currentlyPlayingPostId: Long? = null
    private var currentAudioHolder: PostViewHolder? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null

    // Кэш для последних значений, чтобы избежать лишних обновлений UI
    private var lastCurrentPosition: Long = 0
    private var lastTotalDuration: Long = 0
    private var lastProgress: Int = -1
    private var lastDurationText: String = ""

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
            if (isPlaying) startProgressUpdates()
            else stopProgressUpdates()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Audio player error: ${error.message}")
            stopAudioPlayback()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val binding =
            ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder position=$position")

        // Очищаем Glide запросы перед привязкой
        Glide.with(holder.itemView).clear(holder.binding.ivImage)
        Glide.with(holder.itemView).clear(holder.binding.ivVideoPreview)

        // Сбрасываем состояние аудио для этого холдера
        holder.resetAudioState()

        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        releaseAudioPlayer()
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                // Обновляем состояние только если есть активный аудио плеер
                if (currentlyPlayingPostId != null && currentAudioHolder != null) {
                    currentAudioHolder?.updateAudioPlaybackState()
                }
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

        // Сбрасываем кэшированные значения
        lastCurrentPosition = 0
        lastTotalDuration = 0
        lastProgress = -1
        lastDurationText = ""

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

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentPostId: Long? = null
        private var isAudioPlayingForThisPost = false
            set(value) {
                if (field != value) {
                    field = value
                    updateAudioPlaybackState()
                }
            }

        // Кэш для оптимизации обновлений UI
        private var lastCachedProgress: Int = -1
        private var lastCachedDurationText: String = ""

        init {
            binding.seekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser && audioPlayer != null && currentlyPlayingPostId == currentPostId) {
                        val duration = audioPlayer?.duration ?: 0
                        if (duration > 0) {
                            val seekPosition = ((progress / 100f) * duration).toLong()
                            audioPlayer?.seekTo(seekPosition)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            binding.btnPlayPause.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    if (post.attachment?.type == AttachmentType.AUDIO) {
                        toggleAudioPlayback(post)
                    }
                }
            }

            binding.btnShare.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    onShare(post)
                }
            }
        }

        fun resetAudioState() {
            lastCachedProgress = -1
            lastCachedDurationText = ""
        }

        fun bind(post: Post) = with(binding) {
            currentPostId = post.id

            // --- Аватар ---
            if (!post.authorAvatar.isNullOrBlank()) {
                Glide.with(itemView)
                    .load(post.authorAvatar)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(80, 80)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                // Создаем LetterAvatarDrawable только если нет аватара
                val name = post.author
                val firstLetter = name.firstOrNull()?.toString() ?: "?"
                val letterDrawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(
                        itemView.context,
                        R.color.purple_primary
                    )
                ).apply { setBounds(0, 0, 100, 100) }
                ivAvatar.setImageDrawable(letterDrawable)
            }

            // --- Дата ---
            tvDate.text = post.published.formatForDisplay()

            // --- Контент ---
            if (post.content.isBlank()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            // --- Лайки ---
            val likesCount = post.likeOwnerIds.size.toString()
            if (tvLikes.text != likesCount) {
                tvLikes.text = likesCount
            }

            val likeIcon = if (post.likedByMe) R.drawable.ic_liked else R.drawable.ic_like
            if (ivLike.tag != likeIcon) {
                ivLike.setImageResource(likeIcon)
                ivLike.tag = likeIcon
            }

            btnLike.setOnClickListener {
                animateLike()
                onLike(post)
            }

            // --- Медиа контент ---
            mediaContainer.visibility = View.GONE
            ivImage.visibility = View.GONE
            ivVideoPreview.visibility = View.GONE
            ivPlay?.visibility = View.GONE
            audioPlayer.visibility = View.GONE

            val attachment = post.attachment

            if (attachment != null) {
                mediaContainer.visibility = View.VISIBLE

                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        ivImage.visibility = View.VISIBLE
                        Glide.with(itemView)
                            .load(attachment.url)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(400, 400)
                            .centerCrop()
                            .into(ivImage)
                        ivImage.setOnClickListener { onOpen(post) }
                    }

                    AttachmentType.VIDEO -> {
                        val videoUrl = attachment.url
                        ivVideoPreview.visibility = View.VISIBLE
                        ivPlay?.visibility = View.VISIBLE

                        // Загружаем превью видео асинхронно
                        Glide.with(itemView)
                            .load(videoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(400, 400)
                            .frame(1000000) // Один кадр на 1 секунде
                            .centerCrop()
                            .into(ivVideoPreview)

                        ivVideoPreview.setOnClickListener {
                            onPlayMedia(videoUrl, true)
                        }
                        ivPlay?.setOnClickListener {
                            onPlayMedia(videoUrl, true)
                        }
                    }

                    AttachmentType.AUDIO -> {
                        audioPlayer.visibility = View.VISIBLE
                        mediaContainer.visibility = View.GONE

                        val isCurrentlyPlaying = (currentlyPlayingPostId == post.id)

                        // Обновляем состояние только если изменилось
                        if (isAudioPlayingForThisPost != isCurrentlyPlaying) {
                            isAudioPlayingForThisPost = isCurrentlyPlaying
                        }

                        // Если это текущий воспроизводящийся пост, обновляем UI
                        if (isCurrentlyPlaying) {
                            if (currentAudioHolder != this@PostViewHolder) {
                                currentAudioHolder = this@PostViewHolder
                            }
                            updateAudioPlaybackState()
                        } else {
                            // Сбрасываем UI только если нужно
                            if (btnPlayPause.drawable?.constantState !=
                                ContextCompat.getDrawable(itemView.context, R.drawable.ic_audio_play)?.constantState) {
                                btnPlayPause.setImageResource(R.drawable.ic_audio_play)
                            }

                            val defaultDurationText = "00:00 / 00:00"
                            if (tvAudioDuration.text != defaultDurationText) {
                                tvAudioDuration.text = defaultDurationText
                            }

                            if (seekBar.progress != 0) {
                                seekBar.progress = 0
                            }
                        }
                    }

                    else -> mediaContainer.visibility = View.GONE
                }
            }

            // --- Меню ---
            if (isOwnedByUser(post)) {
                btnMore.visibility = View.VISIBLE
                btnMore.setOnClickListener { onMenu(post, btnMore) }
            } else {
                btnMore.visibility = View.GONE
            }

            root.setOnClickListener { onOpen(post) }
        }

        private fun animateLike() {
            val scaleX = ObjectAnimator.ofFloat(binding.ivLike, "scaleX", 1f, 1.4f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.ivLike, "scaleY", 1f, 1.4f, 1f)
            val rotation = ObjectAnimator.ofFloat(binding.ivLike, "rotation", 0f, -8f, 8f, 0f)

            scaleX.interpolator = OvershootInterpolator()
            scaleY.interpolator = OvershootInterpolator()

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, rotation)
                duration = 300
                start()
            }
        }

        fun toggleAudioPlayback(post: Post) {
            if (currentlyPlayingPostId == post.id) {
                audioPlayer?.pause()
                currentlyPlayingPostId = null
                currentAudioHolder = null
                isAudioPlayingForThisPost = false
                stopProgressUpdates()
            } else {
                if (audioPlayer == null) {
                    audioPlayer = ExoPlayer.Builder(itemView.context).build().also {
                        it.addListener(playerListener)
                    }
                }

                val audioUrl = post.attachment?.url
                if (audioUrl.isNullOrBlank()) return

                val mediaItem = MediaItem.fromUri(android.net.Uri.parse(audioUrl))

                audioPlayer?.setMediaItem(mediaItem)
                audioPlayer?.prepare()
                audioPlayer?.play()

                currentlyPlayingPostId = post.id
                currentAudioHolder = this
                isAudioPlayingForThisPost = true

                startProgressUpdates()
            }
        }

        fun updateAudioPlaybackState() {
            // Быстрая проверка - если это не текущий пост, выходим
            if (currentlyPlayingPostId != currentPostId) return

            val player = audioPlayer ?: return

            try {
                val isPlaying = player.isPlaying

                // Обновляем иконку только если изменилось состояние
                val expectedIcon = if (isPlaying) R.drawable.ic_audio_pause_24 else R.drawable.ic_audio_play
                if (binding.btnPlayPause.tag != expectedIcon) {
                    binding.btnPlayPause.setImageResource(expectedIcon)
                    binding.btnPlayPause.tag = expectedIcon
                }

                val total = player.duration
                if (total <= 0) return

                val current = player.currentPosition

                // Вычисляем прогресс (0-100)
                val progress = ((current.toFloat() / total) * 100).toInt()

                // Обновляем SeekBar только если прогресс изменился значимо
                if (kotlin.math.abs(progress - lastCachedProgress) >= SEEK_BAR_UPDATE_THRESHOLD) {
                    if (binding.seekBar.progress != progress) {
                        binding.seekBar.progress = progress
                    }
                    lastCachedProgress = progress
                }

                // Обновляем текст длительности
                val durationText = formatDuration(current) + " / " + formatDuration(total)
                if (durationText != lastCachedDurationText) {
                    if (binding.tvAudioDuration.text != durationText) {
                        binding.tvAudioDuration.text = durationText
                    }
                    lastCachedDurationText = durationText
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating audio state: ${e.message}")
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

        // Добавляем быструю проверку для ускорения DiffUtil
        override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
            if (oldItem.likedByMe != newItem.likedByMe) return "like_changed"
            if (oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size) return "likes_count_changed"
            return super.getChangePayload(oldItem, newItem)
        }
    }
}