package ru.netology.nework.ui.post

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentPostDetailBinding
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.UserPreview
import ru.netology.nework.utils.DateUtils.formatForDisplay
import ru.netology.nework.utils.LetterAvatarDrawable
import ru.netology.nework.viewmodel.PostDetailViewModel

private const val TAG = "PostDetailFragment"
private const val PROGRESS_UPDATE_INTERVAL_MS = 200L

@AndroidEntryPoint
class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private var mapView: MapView? = null

    // Audio player
    private var audioPlayer: ExoPlayer? = null
    private var currentAudioUrl: String? = null
    private var isAudioPlaying = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    updateAudioPlaybackState()
                }
                Player.STATE_ENDED -> {
                    stopAudioPlayback()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateAudioPlaybackState()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getLong("postId") ?: run {
            Log.e(TAG, "No postId in arguments")
            findNavController().navigateUp()
            return
        }
        Log.d(TAG, "Received postId = $postId")

        setupSeekBar()
        setupAudioButton()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.loadPost(postId)

        viewModel.post.observe(viewLifecycleOwner) { post ->
            Log.d(TAG, "post observed: $post")
            if (post == null) {
                Log.e(TAG, "Post is null, finishing")
                Toast.makeText(requireContext(), "Пост не найден", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@observe
            }

            Log.d(TAG, "Post loaded: id=${post.id}, author=${post.author}, content=${post.content}")

            loadAvatar(binding.ivAvatar, post.authorAvatar, post.author)
            binding.tvAuthorName.text = post.author

            val authorLogin = post.users?.get(post.authorId)?.name ?: post.author
            binding.tvAuthorLogin.text = "@$authorLogin"
            binding.tvAuthorLogin.visibility = View.VISIBLE

            binding.tvPostDate.text = post.published.formatForDisplay()
            binding.tvPostContent.text = post.content

            updateMediaPreview(post.attachment)

            if (post.coords != null) {
                Log.d(TAG, "Post has coordinates: ${post.coords}")
                showMap(post.coords.lat, post.coords.lng)
            } else {
                binding.mapContainer.visibility = View.GONE
            }
        }

        viewModel.likers.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "likers observed: size=${users.size}")
            buildCarousel(
                container = binding.llLikers,
                parentLayout = binding.likersLayout,
                users = users,
                iconResId = R.drawable.ic_liked,
                onPlusClick = null
            )
        }

        viewModel.mentioned.observe(viewLifecycleOwner) { users ->
            Log.d(TAG, "mentioned observed: size=${users.size}")
            buildCarousel(
                container = binding.llMentioned,
                parentLayout = binding.mentionedLayout,
                users = users,
                iconResId = R.drawable.ic_mentioned,
                onPlusClick = { openUserSelection() }
            )
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && audioPlayer != null && currentAudioUrl != null) {
                    val duration = audioPlayer?.duration ?: 0
                    val seekPosition = ((progress / 100f) * duration).toLong()
                    audioPlayer?.seekTo(seekPosition)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAudioButton() {
        binding.btnPlayPause.setOnClickListener {
            toggleAudioPlayback()
        }
    }

    private fun toggleAudioPlayback() {
        if (currentAudioUrl == null) return

        if (isAudioPlaying) {
            audioPlayer?.pause()
            isAudioPlaying = false
            stopProgressUpdates()
        } else {
            if (audioPlayer == null) {
                audioPlayer = ExoPlayer.Builder(requireContext()).build().also {
                    it.addListener(playerListener)
                }
            }
            val mediaItem = MediaItem.fromUri(Uri.parse(currentAudioUrl))
            audioPlayer?.setMediaItem(mediaItem)
            audioPlayer?.prepare()
            audioPlayer?.play()
            isAudioPlaying = true
            startProgressUpdates()
        }
        updateAudioPlaybackState()
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                updateAudioPlaybackState()
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
        currentAudioUrl?.let {
            isAudioPlaying = false
            updateAudioPlaybackState()
        }
    }

    private fun releaseAudioPlayer() {
        stopProgressUpdates()
        audioPlayer?.removeListener(playerListener)
        audioPlayer?.release()
        audioPlayer = null
        currentAudioUrl = null
        isAudioPlaying = false
    }

    private fun updateAudioPlaybackState() {
        val player = audioPlayer ?: return
        val isPlaying = player.isPlaying
        binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_audio_pause_24 else R.drawable.ic_audio_play)
        val current = player.currentPosition
        val total = player.duration
        if (total > 0) {
            binding.seekBar.progress = ((current.toFloat() / total) * 100).toInt()
            binding.tvAudioDuration.text = formatDuration(current) + " / " + formatDuration(total)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showMap(lat: Double, lng: Double) {
        binding.mapContainer.visibility = View.VISIBLE
        binding.mapContainer.removeAllViews()

        mapView = MapView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        binding.mapContainer.addView(mapView)

        val point = Point(lat, lng)

        mapView?.map?.move(
            CameraPosition(point, 16f, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )

        try {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin)
            val bitmap = Bitmap.createBitmap(
                drawable?.intrinsicWidth ?: 48,
                drawable?.intrinsicHeight ?: 48,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable?.setBounds(0, 0, canvas.width, canvas.height)
            drawable?.draw(canvas)

            val imageProvider = ImageProvider.fromBitmap(bitmap)
            val placemark = mapView?.map?.mapObjects?.addPlacemark(point, imageProvider)
            placemark?.setOpacity(1.0f)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating marker from vector", e)
            val fallbackPlacemark = mapView?.map?.mapObjects?.addPlacemark(point)
            fallbackPlacemark?.setOpacity(1.0f)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        releaseAudioPlayer()
        super.onStop()
    }

    private fun buildCarousel(
        container: LinearLayout,
        parentLayout: View,
        users: List<UserPreview>,
        iconResId: Int,
        onPlusClick: (() -> Unit)?
    ) {
        if (users.isEmpty()) {
            Log.d(TAG, "No users, hiding carousel")
            parentLayout.visibility = View.GONE
            return
        }
        Log.d(TAG, "Building carousel with ${users.size} users")
        parentLayout.visibility = View.VISIBLE
        container.removeAllViews()

        val avatarSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        val overlap = resources.getDimensionPixelSize(R.dimen.avatar_overlap)

        val iconMarginEnd = resources.getDimensionPixelSize(R.dimen.carousel_icon_margin_end)
        val countMarginEnd = resources.getDimensionPixelSize(R.dimen.carousel_count_margin_end)
        val firstAvatarMarginStart = resources.getDimensionPixelSize(R.dimen.carousel_avatar_first_margin_start)

        val iconView = ImageView(requireContext()).apply {
            setImageResource(iconResId)
            layoutParams = ViewGroup.MarginLayoutParams(avatarSize, avatarSize).apply {
                marginEnd = iconMarginEnd
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        container.addView(iconView)

        val countView = TextView(requireContext()).apply {
            text = users.size.toString()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                avatarSize
            ).apply {
                marginEnd = countMarginEnd
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_primary))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(countView)

        val visibleCount = minOf(users.size, 5)
        val extraCount = users.size - 5

        for (i in 0 until visibleCount) {
            val user = users[i]
            val avatarView = createAvatarView(user)
            val layoutParams = avatarView.layoutParams as ViewGroup.MarginLayoutParams
            if (i > 0) {
                layoutParams.marginStart = overlap
            } else {
                layoutParams.marginStart = firstAvatarMarginStart
            }
            avatarView.layoutParams = layoutParams
            container.addView(avatarView)
        }

        if (extraCount > 0 && onPlusClick != null) {
            val plusButton = createPlusButton(onPlusClick)
            val layoutParams = plusButton.layoutParams as ViewGroup.MarginLayoutParams
            if (visibleCount > 0) {
                layoutParams.marginStart = overlap
            }
            plusButton.layoutParams = layoutParams
            container.addView(plusButton)
        }
    }

    private fun createAvatarView(user: UserPreview): View {
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.avatar_stroke_width)
        val avatarSize = resources.getDimensionPixelSize(R.dimen.avatar_size)

        val container = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(avatarSize, avatarSize)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_white_stroke)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, avatarSize, avatarSize)
                }
            }
            clipToOutline = true
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                avatarSize - 2 * strokeWidth,
                avatarSize - 2 * strokeWidth
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val size = avatarSize - 2 * strokeWidth
                    outline.setOval(0, 0, size, size)
                }
            }
            clipToOutline = true

            loadAvatar(this, user.avatar, user.name)
        }
        container.addView(imageView)
        return container
    }

    private fun createPlusButton(onClick: () -> Unit): View {
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.avatar_stroke_width)
        val avatarSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        val innerSize = avatarSize - 2 * strokeWidth

        val container = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(avatarSize, avatarSize)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_white_stroke)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, avatarSize, avatarSize)
                }
            }
            clipToOutline = true
            setOnClickListener { onClick() }
        }

        val innerCircle = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                gravity = android.view.Gravity.CENTER
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_purple)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, innerSize, innerSize)
                }
            }
            clipToOutline = true
        }

        val plusIcon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_plus)
            layoutParams = FrameLayout.LayoutParams(innerSize / 2, innerSize / 2).apply {
                gravity = android.view.Gravity.CENTER
            }
        }

        innerCircle.addView(plusIcon)
        container.addView(innerCircle)
        return container
    }

    private fun loadAvatar(imageView: ImageView, avatarUrl: String?, userName: String) {
        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_account_circle)
                .error(R.drawable.ic_account_circle)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        } else {
            val firstLetter = userName.firstOrNull()?.toString() ?: "?"
            val drawable = LetterAvatarDrawable(
                letter = firstLetter,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.purple_primary)
            )
            imageView.setImageDrawable(drawable)
        }
    }

    private fun openUserSelection() {
        Toast.makeText(requireContext(), "Открыть выбор пользователей", Toast.LENGTH_SHORT).show()
    }

    private fun updateMediaPreview(attachment: ru.netology.nework.model.Attachment?) {
        if (attachment == null) {
            binding.mediaContainer.visibility = View.GONE
            return
        }

        binding.mediaContainer.visibility = View.VISIBLE
        binding.imagePreview.visibility = View.GONE
        binding.videoContainer.visibility = View.GONE
        binding.audioPlayer.visibility = View.GONE

        when (attachment.type) {
            AttachmentType.IMAGE -> {
                binding.imagePreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(attachment.url)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.imagePreview)
            }

            AttachmentType.VIDEO -> {
                binding.videoContainer.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = ThumbnailUtils.createVideoThumbnail(
                            attachment.url,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )

                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                binding.videoPreview.setImageBitmap(bitmap)
                                binding.ivPlay.visibility = View.VISIBLE
                            } else {
                                Glide.with(this@PostDetailFragment)
                                    .load(attachment.url)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .frame(1000000)
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_play_circle_filled)
                                    .error(R.drawable.ic_play_circle_filled)
                                    .into(binding.videoPreview)
                                binding.ivPlay.visibility = View.VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading video thumbnail", e)
                        withContext(Dispatchers.Main) {
                            binding.videoPreview.setImageResource(R.drawable.ic_play_circle_filled)
                            binding.ivPlay.visibility = View.VISIBLE
                        }
                    }
                }

                binding.videoContainer.setOnClickListener {
                    // TODO: Implement video playback
                    Toast.makeText(requireContext(), "Видео пока не поддерживается", Toast.LENGTH_SHORT).show()
                }
            }

            AttachmentType.AUDIO -> {
                binding.audioPlayer.visibility = View.VISIBLE
                currentAudioUrl = attachment.url

                // Reset audio state
                if (isAudioPlaying) {
                    stopAudioPlayback()
                }
                binding.btnPlayPause.setImageResource(R.drawable.ic_audio_play)
                binding.tvAudioDuration.text = formatDuration(0) + " / " + formatDuration(0)
                binding.seekBar.progress = 0
            }
        }
    }

    override fun onDestroyView() {
        mapView?.onStop()
        mapView = null
        releaseAudioPlayer()
        super.onDestroyView()
        _binding = null
    }
}