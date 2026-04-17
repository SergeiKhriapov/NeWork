package ru.netology.nework.ui.events

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
import ru.netology.nework.databinding.ItemEventBinding
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Event
import ru.netology.nework.model.EventType
import ru.netology.nework.utils.DateUtils.formatForDisplay
import ru.netology.nework.utils.LetterAvatarDrawable

private const val PROGRESS_UPDATE_INTERVAL_MS = 200L

class EventAdapter(
    private val onLike: (Event) -> Unit,
    private val onParticipate: (Event) -> Unit,
    private val onOpen: (Event) -> Unit,
    private val onMenu: (Event, View) -> Unit,
    private val onPlayMedia: (String, Boolean) -> Unit,
    private val onShare: (Event) -> Unit,
    private val isOwnedByUser: (Event) -> Boolean
) : ListAdapter<Event, EventAdapter.EventViewHolder>(DiffCallback) {

    private var audioPlayer: ExoPlayer? = null
    private var currentlyPlayingEventId: Long? = null
    private var currentAudioHolder: EventViewHolder? = null

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
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
        currentlyPlayingEventId = null
        currentAudioHolder?.updateAudioPlaybackState()
        currentAudioHolder = null
    }

    private fun releaseAudioPlayer() {
        stopProgressUpdates()
        audioPlayer?.removeListener(playerListener)
        audioPlayer?.release()
        audioPlayer = null
        currentlyPlayingEventId = null
        currentAudioHolder = null
    }

    inner class EventViewHolder(private val b: ItemEventBinding) :
        RecyclerView.ViewHolder(b.root) {

        private var currentEventId: Long? = null
        private var isAudioPlayingForThisEvent = false
            set(value) {
                if (field != value) {
                    field = value
                    updateAudioPlaybackState()
                }
            }

        init {
            b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && audioPlayer != null && currentlyPlayingEventId == currentEventId) {
                        val duration = audioPlayer?.duration ?: 0
                        val seekPosition = ((progress / 100f) * duration).toLong()
                        audioPlayer?.seekTo(seekPosition)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            b.btnPlayPause.setOnClickListener {
                val event = getItem(absoluteAdapterPosition)
                if (event.attachment?.type == AttachmentType.AUDIO) {
                    toggleAudioPlayback(event)
                }
            }

            b.btnShare.setOnClickListener {
                val event = getItem(absoluteAdapterPosition)
                onShare(event)
            }
        }

        fun bind(event: Event) = with(b) {
            currentEventId = event.id

            tvAuthor.text = event.author

            if (!event.authorAvatar.isNullOrBlank()) {
                Glide.with(itemView)
                    .load(event.authorAvatar)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                val name = event.author
                val firstLetter = name.firstOrNull()?.toString() ?: "?"
                val letterDrawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.purple_primary)
                ).apply { setBounds(0, 0, 100, 100) }
                ivAvatar.setImageDrawable(letterDrawable)
            }

            tvDate.text = event.published.formatForDisplay()

            tvEventStatus.text = when (event.type) {
                EventType.OFFLINE -> "Offline"
                EventType.ONLINE -> "Online"
            }
            tvEventDate.text = event.datetime.formatForDisplay()

            if (event.content.isBlank()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = event.content
            }

            if (!event.link.isNullOrBlank()) {
                tvLink.visibility = View.VISIBLE
                tvLink.text = event.link
            } else {
                tvLink.visibility = View.GONE
            }

            tvLikes.text = event.likeOwnerIds.size.toString()
            ivLike.setImageResource(
                if (event.likedByMe) R.drawable.ic_liked else R.drawable.ic_like
            )

            tvParticipants.text = event.participantsIds.size.toString()
            ivParticipants.setImageResource(
                if (event.participatedByMe) R.drawable.ic_people_outline else R.drawable.ic_mentioned
            )

            btnLike.setOnClickListener {
                animateView(b.ivLike)
                onLike(event)
            }

            btnParticipants.setOnClickListener {
                animateView(b.ivParticipants)
                onParticipate(event)
            }

            mediaContainer.visibility = View.GONE
            ivImage.visibility = View.GONE
            ivVideoPreview.visibility = View.GONE
            ivPlay?.visibility = View.GONE
            audioPlayer.visibility = View.GONE

            Glide.with(itemView).clear(ivImage)
            ivVideoPreview.let { Glide.with(itemView).clear(it) }

            val attachment = event.attachment

            if (attachment != null) {
                mediaContainer.visibility = View.VISIBLE

                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        ivImage.visibility = View.VISIBLE
                        Glide.with(itemView)
                            .load(attachment.url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivImage)
                        ivImage.setOnClickListener { onOpen(event) }
                    }

                    AttachmentType.VIDEO -> {
                        val videoUrl = attachment.url
                        ivVideoPreview.visibility = View.VISIBLE
                        ivPlay?.visibility = View.VISIBLE

                        Glide.with(itemView)
                            .load(videoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .frame(1000000)
                            .centerCrop()
                            .into(ivVideoPreview)

                        ivVideoPreview.setOnClickListener { onPlayMedia(videoUrl, true) }
                        ivPlay?.setOnClickListener { onPlayMedia(videoUrl, true) }
                    }

                    AttachmentType.AUDIO -> {
                        audioPlayer.visibility = View.VISIBLE
                        mediaContainer.visibility = View.GONE

                        isAudioPlayingForThisEvent = (currentlyPlayingEventId == event.id)

                        if (isAudioPlayingForThisEvent) {
                            currentAudioHolder = this@EventViewHolder
                            updateAudioPlaybackState()
                        } else {
                            btnPlayPause.setImageResource(R.drawable.ic_audio_play)
                            tvAudioDuration.text = "00:00 / 00:00"
                            seekBar.progress = 0
                        }
                    }

                    else -> mediaContainer.visibility = View.GONE
                }
            }

            if (isOwnedByUser(event)) {
                btnMore.visibility = View.VISIBLE
                btnMore.setOnClickListener { onMenu(event, btnMore) }
            } else {
                btnMore.visibility = View.GONE
            }

            root.setOnClickListener { onOpen(event) }
        }

        private fun animateView(view: View) {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.4f, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.4f, 1f)
            val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, -8f, 8f, 0f)

            scaleX.interpolator = OvershootInterpolator()
            scaleY.interpolator = OvershootInterpolator()

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, rotation)
                duration = 300
                start()
            }
        }

        fun toggleAudioPlayback(event: Event) {
            if (currentlyPlayingEventId == event.id) {
                audioPlayer?.pause()
                currentlyPlayingEventId = null
                currentAudioHolder = null
                isAudioPlayingForThisEvent = false
                stopProgressUpdates()
            } else {
                if (audioPlayer == null) {
                    audioPlayer = ExoPlayer.Builder(itemView.context).build().also {
                        it.addListener(playerListener)
                    }
                }

                val mediaItem = MediaItem.fromUri(android.net.Uri.parse(event.attachment?.url ?: return))

                audioPlayer?.setMediaItem(mediaItem)
                audioPlayer?.prepare()
                audioPlayer?.play()

                currentlyPlayingEventId = event.id
                currentAudioHolder = this
                isAudioPlayingForThisEvent = true

                startProgressUpdates()
            }
        }

        fun updateAudioPlaybackState() {
            if (currentlyPlayingEventId != currentEventId) return

            val player = audioPlayer ?: return
            val isPlaying = player.isPlaying

            b.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_audio_pause_24 else R.drawable.ic_audio_play
            )

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

    object DiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}