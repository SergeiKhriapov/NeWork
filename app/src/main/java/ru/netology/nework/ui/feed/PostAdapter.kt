package ru.netology.nework.ui.feed

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import ru.netology.nework.R
import ru.netology.nework.databinding.ItemPostBinding
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post
import ru.netology.nework.utils.DateUtils.formatForDisplay
import ru.netology.nework.utils.LetterAvatarDrawable

private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
private const val SEEK_BAR_UPDATE_THRESHOLD = 2

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
            stopAudioPlayback()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Glide.with(holder.itemView).clear(holder.binding.ivImage)
        Glide.with(holder.itemView).clear(holder.binding.ivVideoPreview)
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

        private var lastCachedProgress: Int = -1
        private var lastCachedDurationText: String = ""

        init {
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

            tvAuthor.text = post.author

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
                val firstLetter = post.author.firstOrNull()?.toString() ?: "?"
                val letterDrawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.purple_primary)
                )
                ivAvatar.setImageDrawable(letterDrawable)
            }

            tvDate.text = post.published.formatForDisplay()

            if (post.content.isBlank()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            tvLikes.text = post.likeOwnerIds.size.toString()
            val likeIcon = if (post.likedByMe) R.drawable.ic_liked else R.drawable.ic_like
            ivLike.setImageResource(likeIcon)

            btnLike.setOnClickListener {
                animateLike()
                onLike(post)
            }

            mediaContainer.visibility = View.GONE
            ivImage.visibility = View.GONE
            ivVideoPreview.visibility = View.GONE
            ivPlay.visibility = View.GONE
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
                            .centerCrop()
                            .into(ivImage)
                        ivImage.setOnClickListener { onOpen(post) }
                    }

                    AttachmentType.VIDEO -> {
                        ivVideoPreview.visibility = View.VISIBLE
                        ivPlay.visibility = View.VISIBLE
                        Glide.with(itemView)
                            .load(attachment.url)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .frame(1000000)
                            .centerCrop()
                            .into(ivVideoPreview)
                        ivVideoPreview.setOnClickListener { onPlayMedia(attachment.url, true) }
                        ivPlay.setOnClickListener { onPlayMedia(attachment.url, true) }
                    }

                    AttachmentType.AUDIO -> {
                        audioPlayer.visibility = View.VISIBLE
                        mediaContainer.visibility = View.GONE
                        val isCurrentlyPlaying = (currentlyPlayingPostId == post.id)
                        if (isAudioPlayingForThisPost != isCurrentlyPlaying) {
                            isAudioPlayingForThisPost = isCurrentlyPlaying
                        }
                        if (isCurrentlyPlaying) {
                            if (currentAudioHolder != this@PostViewHolder) {
                                currentAudioHolder = this@PostViewHolder
                            }
                            updateAudioPlaybackState()
                        } else {
                            btnPlayPause.setImageResource(R.drawable.ic_audio_play)
                            tvAudioDuration.text = "00:00 / 00:00"
                            seekBar.progress = 0
                        }
                    }
                }
            }

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
                val audioUrl = post.attachment?.url ?: return
                audioPlayer?.setMediaItem(MediaItem.fromUri(android.net.Uri.parse(audioUrl)))
                audioPlayer?.prepare()
                audioPlayer?.play()
                currentlyPlayingPostId = post.id
                currentAudioHolder = this
                isAudioPlayingForThisPost = true
                startProgressUpdates()
            }
        }

        fun updateAudioPlaybackState() {
            if (currentlyPlayingPostId != currentPostId) return
            val player = audioPlayer ?: return

            try {
                val isPlaying = player.isPlaying
                val expectedIcon = if (isPlaying) R.drawable.ic_audio_pause_24 else R.drawable.ic_audio_play
                if (binding.btnPlayPause.tag != expectedIcon) {
                    binding.btnPlayPause.setImageResource(expectedIcon)
                    binding.btnPlayPause.tag = expectedIcon
                }

                val total = player.duration
                if (total <= 0) return

                val current = player.currentPosition
                val progress = ((current.toFloat() / total) * 100).toInt()

                if (kotlin.math.abs(progress - lastCachedProgress) >= SEEK_BAR_UPDATE_THRESHOLD) {
                    if (binding.seekBar.progress != progress) {
                        binding.seekBar.progress = progress
                    }
                    lastCachedProgress = progress
                }

                val durationText = formatDuration(current) + " / " + formatDuration(total)
                if (durationText != lastCachedDurationText) {
                    binding.tvAudioDuration.text = durationText
                    lastCachedDurationText = durationText
                }
            } catch (e: Exception) {
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
        override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
            if (oldItem.likedByMe != newItem.likedByMe) return "like_changed"
            if (oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size) return "likes_count_changed"
            return super.getChangePayload(oldItem, newItem)
        }
    }
}