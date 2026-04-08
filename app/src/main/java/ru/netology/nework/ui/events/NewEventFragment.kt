package ru.netology.nework.ui.events

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Typeface
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.app.OnPostActionListener
import ru.netology.nework.databinding.FragmentNewEventBinding
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.EventType
import ru.netology.nework.model.User
import ru.netology.nework.ui.users.REQUEST_KEY
import ru.netology.nework.ui.users.SELECTED_USERS_KEY
import ru.netology.nework.utils.LetterAvatarDrawable
import ru.netology.nework.viewmodel.UsersViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val TAG = "NewEventFragment"
private const val LOCATION_REQUEST_KEY = "location_request"

@AndroidEntryPoint
class NewEventFragment : Fragment(), OnPostActionListener {

    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewEventViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private var mapView: MapView? = null

    private var currentPhotoPath: String? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath != null) {
                val file = File(currentPhotoPath!!)
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                handleMediaResult(uri, AttachmentType.IMAGE)
            } else {
                Toast.makeText(requireContext(), "Не удалось сделать фото", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleMediaResult(it, AttachmentType.IMAGE) }
        }

    private val galleryVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleMediaResult(it, AttachmentType.VIDEO) }
        }

    private val audioLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleMediaResult(it, AttachmentType.AUDIO) }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera()
            else Toast.makeText(requireContext(), "Необходимо разрешение на камеру", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "=== onViewCreated START ===")

        if (!viewModel.isArgumentsLoaded()) {
            loadArguments()
            viewModel.markArgumentsLoaded()
        }

        setupObservers()
        setupListeners()
        setupResultListeners()
        setupLocalFAB()

        binding.fabEventOptions.post {
            binding.fabEventOptions.show()
            binding.fabEventOptions.bringToFront()
            Log.d(TAG, "Local FAB visibility after post: ${binding.fabEventOptions.visibility}")
        }

        Log.d(TAG, "=== onViewCreated END ===")
    }

    private fun loadArguments() {
        arguments?.let { args ->
            val eventId = args.getLong("eventId", -1)

            Log.d(TAG, "loadArguments: eventId = $eventId")
            Log.d(TAG, "All arguments keys: ${args.keySet().joinToString()}")

            if (eventId != -1L) {
                val content = args.getString("content", "")
                val attachmentUrl = args.getString("attachmentUrl", "")
                val attachmentTypeStr = args.getString("attachmentType", "")
                val attachment = if (attachmentUrl.isNotBlank() && attachmentTypeStr.isNotBlank()) {
                    try {
                        Attachment(attachmentUrl, AttachmentType.valueOf(attachmentTypeStr))
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val lat = args.getDouble("lat", 0.0)
                val lng = args.getDouble("lng", 0.0)
                val coords = if (lat != 0.0 && lng != 0.0) Coordinates(lat, lng) else null

                val eventTypeStr = args.getString("eventType", "OFFLINE")
                val eventType = try {
                    EventType.valueOf(eventTypeStr)
                } catch (e: Exception) {
                    EventType.OFFLINE
                }

                val eventDateTimeStr = args.getString("eventDateTime")
                Log.d(TAG, "loadArguments: eventDateTimeStr = $eventDateTimeStr")

                val eventDateTime = eventDateTimeStr?.let {
                    try {
                        OffsetDateTime.parse(it).toLocalDateTime()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing eventDateTime", e)
                        null
                    }
                }

                Log.d(TAG, "loadArguments: parsed eventDateTime = $eventDateTime")

                // ВАЖНО: speakerIds - это спикеры
                val speakerIds = args.getLongArray("speakerIds")?.toSet() ?: emptySet()

                viewModel.initEditing(
                    eventId, content, attachment, coords,
                    eventType, eventDateTime, speakerIds
                )

                Log.d(TAG, "After initEditing: viewModel.eventDateTime.value = ${viewModel.eventDateTime.value}")
                Log.d(TAG, "After initEditing: viewModel.speakerIds.value = ${viewModel.speakerIds.value}")
            } else {
                viewModel.initNew()
            }
        }
    }

    private fun setupObservers() {
        viewModel.eventText.observe(viewLifecycleOwner) { text ->
            if (binding.editTextEvent.text.toString() != text) {
                binding.editTextEvent.setText(text)
            }
        }

        binding.editTextEvent.doAfterTextChanged { text ->
            val newText = text?.toString() ?: ""
            if (viewModel.eventText.value != newText) {
                viewModel.setText(newText)
            }
        }

        viewModel.attachment.distinctUntilChanged()
            .observe(viewLifecycleOwner) { updateMediaPreview(it) }

        viewModel.coordinates.observe(viewLifecycleOwner) { coords ->
            if (coords != null) {
                binding.locationContainer.visibility = View.VISIBLE
                binding.tvLocationInfo.visibility = View.VISIBLE
                binding.btnRemoveLocation.visibility = View.VISIBLE
                showMap(coords.lat, coords.lng)
            } else {
                binding.locationContainer.visibility = View.GONE
                binding.tvLocationInfo.visibility = View.GONE
                binding.btnRemoveLocation.visibility = View.GONE
                mapView = null
            }
        }

        // Наблюдаем за спикерами
        viewModel.speakerIds.observe(viewLifecycleOwner) {
            updateSelectedSpeakersDisplay()
        }

        usersViewModel.users.observe(viewLifecycleOwner) {
            updateSelectedSpeakersDisplay()
        }

        viewModel.eventType.observe(viewLifecycleOwner) { type ->
            // Обновляем UI если нужно
        }

        viewModel.eventDateTime.observe(viewLifecycleOwner) { dateTime ->
            // Обновляем UI если нужно
        }

        viewModel.isEditing.observe(viewLifecycleOwner) { isEditing ->
            val title = if (isEditing) "Edit event" else "New event"
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title
        }

        viewModel.saveCompleted.observe(viewLifecycleOwner) { success ->
            if (success != null && isAdded) {
                Toast.makeText(
                    requireContext(),
                    if (viewModel.isEditing.value == true) "Event updated" else "Event saved",
                    Toast.LENGTH_SHORT
                ).show()
                viewLifecycleOwner.lifecycleScope.launch { delay(100); if (isAdded) findNavController().navigateUp() }
                viewModel.resetSaveCompleted()
            }
        }
    }

    private fun setupListeners() {
        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_camera -> {
                    checkCameraPermissionAndOpen(); true
                }
                R.id.action_attach -> {
                    showAttachmentDialog(); true
                }
                R.id.action_users -> {
                    openSpeakersSelection(); true
                }
                R.id.action_location -> {
                    openLocationPicker(); true
                }
                else -> false
            }
        }

        binding.btnRemove.setOnClickListener { viewModel.setAttachment(null) }
        binding.btnRemoveLocation.setOnClickListener {
            viewModel.setCoordinates(null)
            Toast.makeText(requireContext(), "Местоположение удалено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLocalFAB() {
        try {
            val fab = binding.fabEventOptions
            fab.setOnClickListener {
                Log.d(TAG, "FAB clicked in NewEventFragment")
                openEventOptionsBottomSheet()
            }
            fab.show()
            fab.isEnabled = true
            fab.isClickable = true
            Log.d(TAG, "Local FAB configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "setupLocalFAB error: ${e.message}")
        }
    }

    private fun openEventOptionsBottomSheet() {
        Log.d(TAG, "Opening bottom sheet")
        try {
            val bottomSheet = EventOptionsBottomSheet.newInstance()

            val currentEventType = viewModel.eventType.value
            val currentDateTime = viewModel.eventDateTime.value
            if (currentEventType != null && currentDateTime != null) {
                bottomSheet.setInitialValues(currentEventType, currentDateTime)
            }

            bottomSheet.onOptionsSelected = { eventType, dateTime ->
                Log.d(TAG, "Options selected: type=$eventType, dateTime=$dateTime")
                viewModel.setEventType(eventType)
                viewModel.setEventDateTime(dateTime)
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())
                Toast.makeText(requireContext(), "Event updated: ${eventType.name} at ${dateTime.format(formatter)}", Toast.LENGTH_SHORT).show()
            }
            bottomSheet.show(parentFragmentManager, "event_options")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening bottom sheet: ${e.message}")
        }
    }

    private fun setupResultListeners() {
        setFragmentResultListener(LOCATION_REQUEST_KEY) { _, bundle ->
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")
            if (lat != 0.0 && lng != 0.0) viewModel.setCoordinates(Coordinates(lat, lng))
            else viewModel.setCoordinates(null)
            Toast.makeText(
                requireContext(),
                if (lat != 0.0 && lng != 0.0) "Location selected" else "Location removed",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Получаем выбранных спикеров
        setFragmentResultListener(REQUEST_KEY) { _, bundle ->
            val selectedIds = bundle.getLongArray(SELECTED_USERS_KEY)?.toSet() ?: emptySet()
            Log.d(TAG, "Speakers selected: ${selectedIds.joinToString()}")
            viewModel.setSpeakerIds(selectedIds)
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
        super.onStop()
    }

    private fun openLocationPicker() {
        findNavController().navigate(R.id.locationPickerFragment)
    }

    private fun openSpeakersSelection() {
        findNavController().navigate(
            R.id.userSelectionFragment,
            Bundle().apply {
                putLongArray("selected_ids", viewModel.speakerIds.value?.toLongArray() ?: longArrayOf())
                putString("title", "Select speakers")
                putString("selection_mode", "speakers") // Указываем, что выбираем спикеров
            }
        )
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile == null) {
                Toast.makeText(requireContext(), "Failed to create image file", Toast.LENGTH_SHORT).show()
                return
            }
            currentPhotoPath = photoFile.absolutePath
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(requireContext(), "Camera not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir("Pictures") ?: return null
            if (!storageDir.exists()) storageDir.mkdirs()
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun showAttachmentDialog() {
        val options = arrayOf("Image", "Video", "Audio")
        AlertDialog.Builder(requireContext()).setTitle("Select attachment type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryImageLauncher.launch("image/*")
                    1 -> galleryVideoLauncher.launch("video/*")
                    2 -> audioLauncher.launch("audio/*")
                }
            }.show()
    }

    private fun handleMediaResult(uri: Uri, type: AttachmentType) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return@launch
            val file = File(requireContext().filesDir, "media_${System.currentTimeMillis()}")
            file.outputStream().use { inputStream.copyTo(it) }
            withContext(Dispatchers.Main) {
                viewModel.setAttachment(Attachment(file.absolutePath, type))
            }
        }
    }

    private fun updateMediaPreview(attachment: Attachment?) {
        Glide.with(this).clear(binding.imagePreview)
        Glide.with(this).clear(binding.videoPreview)
        if (attachment == null) {
            binding.imageContainer.visibility = View.GONE
            return
        }

        binding.imageContainer.visibility = View.VISIBLE
        binding.btnRemove.visibility = View.VISIBLE
        binding.imagePreview.visibility = View.GONE
        binding.videoContainer.visibility = View.GONE
        binding.audioPlayer.visibility = View.GONE

        val isLocalFile = !attachment.url.startsWith("http://") && !attachment.url.startsWith("https://")

        when (attachment.type) {
            AttachmentType.IMAGE -> {
                binding.imagePreview.visibility = View.VISIBLE
                val load = if (isLocalFile) Glide.with(this).load(File(attachment.url))
                else Glide.with(this).load(attachment.url)
                load.centerCrop().into(binding.imagePreview)
            }
            AttachmentType.VIDEO -> {
                binding.videoContainer.visibility = View.VISIBLE
                if (isLocalFile) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val bitmap = ThumbnailUtils.createVideoThumbnail(
                            attachment.url,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )
                        withContext(Dispatchers.Main) {
                            binding.videoPreview.setImageBitmap(bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                        }
                    }
                } else {
                    Glide.with(this).load(attachment.url).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .frame(1000000).centerCrop()
                        .placeholder(R.drawable.ic_play_circle_filled).into(binding.videoPreview)
                }
                binding.ivPlay.visibility = View.VISIBLE
            }
            AttachmentType.AUDIO -> {
                binding.audioPlayer.visibility = View.VISIBLE
                binding.btnPlayPause.setOnClickListener {
                    Toast.makeText(requireContext(), "Audio playback", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMap(lat: Double, lng: Double) {
        binding.mapContainer.visibility = View.VISIBLE
        binding.mapContainer.removeAllViews()
        mapView = MapView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            isEnabled = false
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
            mapView?.map?.mapObjects?.addPlacemark(point, ImageProvider.fromBitmap(bitmap))
        } catch (e: Exception) {
            mapView?.map?.mapObjects?.addPlacemark(point)
        }
    }

    // Обновляем отображение выбранных спикеров
    private fun updateSelectedSpeakersDisplay() {
        val allUsers = usersViewModel.users.value ?: return
        val selectedIds = viewModel.speakerIds.value ?: emptySet()
        val selectedSpeakers = allUsers.filter { it.id in selectedIds }

        if (selectedSpeakers.isEmpty()) {
            binding.llSelectedUsers.visibility = View.GONE
            binding.tvMentionedLabel.visibility = View.GONE
            return
        }

        binding.llSelectedUsers.visibility = View.VISIBLE
        binding.tvMentionedLabel.visibility = View.VISIBLE
        binding.tvMentionedLabel.text = "Speakers:" // Меняем текст на "Speakers:"
        binding.llSelectedUsers.removeAllViews()

        val avatarSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        val overlap = resources.getDimensionPixelSize(R.dimen.avatar_overlap)
        val iconMarginEnd = resources.getDimensionPixelSize(R.dimen.mention_icon_margin)
        val countMarginEnd = resources.getDimensionPixelSize(R.dimen.mention_count_margin)

        val iconView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_mentioned) // Иконка для спикеров
            layoutParams = ViewGroup.MarginLayoutParams(avatarSize, avatarSize).apply {
                marginEnd = iconMarginEnd
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        binding.llSelectedUsers.addView(iconView)

        val countView = TextView(requireContext()).apply {
            text = selectedSpeakers.size.toString()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                avatarSize
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.mention_count_margin_start)
                marginEnd = countMarginEnd
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_primary))
            textSize = 14f
            setLineSpacing(6f, 1f)
            letterSpacing = 0.1f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        }
        binding.llSelectedUsers.addView(countView)

        val visibleCount = minOf(selectedSpeakers.size, 5)
        val extraCount = selectedSpeakers.size - 5

        for (i in 0 until visibleCount) {
            val user = selectedSpeakers[i]
            val avatarView = createSpeakerAvatarView(user)
            val layoutParams = avatarView.layoutParams as ViewGroup.MarginLayoutParams
            if (i > 0) {
                layoutParams.marginStart = overlap
            }
            avatarView.layoutParams = layoutParams
            binding.llSelectedUsers.addView(avatarView)
        }

        if (extraCount > 0) {
            val plusButton = createPlusButton {
                openSpeakersSelection()
            }
            val layoutParams = plusButton.layoutParams as ViewGroup.MarginLayoutParams
            if (visibleCount > 0) {
                layoutParams.marginStart = overlap
            }
            plusButton.layoutParams = layoutParams
            binding.llSelectedUsers.addView(plusButton)
        }
    }

    private fun createSpeakerAvatarView(user: User): View {
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
            setOnClickListener {
                showRemoveSpeakerDialog(user)
            }
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

            if (!user.avatar.isNullOrBlank()) {
                Glide.with(this@NewEventFragment)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(this)
            } else {
                val firstLetter = user.name.firstOrNull()?.toString() ?: "?"
                val drawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.purple_primary)
                ).apply {
                    setBounds(0, 0, avatarSize - 2 * strokeWidth, avatarSize - 2 * strokeWidth)
                }
                setImageDrawable(drawable)
            }
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

    private fun showRemoveSpeakerDialog(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Убрать спикера")
            .setMessage("Убрать ${user.name} из спикеров?")
            .setPositiveButton("Да") { _, _ ->
                val currentIds = viewModel.speakerIds.value?.toMutableSet() ?: return@setPositiveButton
                currentIds.remove(user.id)
                viewModel.setSpeakerIds(currentIds)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onPostAction() {
        Log.d(TAG, "=== onPostAction CALLED ===")
        Log.d(TAG, "viewModel.isSaving.value = ${viewModel.isSaving.value}")
        Log.d(TAG, "viewModel.eventDateTime.value = ${viewModel.eventDateTime.value}")
        Log.d(TAG, "viewModel.isEditing.value = ${viewModel.isEditing.value}")
        Log.d(TAG, "viewModel.editingEventId.value = ${viewModel.editingEventId.value}")
        Log.d(TAG, "viewModel.speakerIds.value = ${viewModel.speakerIds.value}")

        if (viewModel.isSaving.value == true) {
            Toast.makeText(requireContext(), "Saving in progress", Toast.LENGTH_SHORT).show()
            return
        }

        val text = binding.editTextEvent.text.toString()
        val eventDateTime = viewModel.eventDateTime.value

        if (eventDateTime == null) {
            Log.e(TAG, "eventDateTime is NULL!")
            Toast.makeText(requireContext(), "Сначала выберите дату и время мероприятия!", Toast.LENGTH_LONG).show()
            return
        }

        if (text.isBlank() && viewModel.attachment.value == null) {
            Toast.makeText(requireContext(), "Введите текст или добавьте медиа", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.isEditing.value == true) {
            val editingEventId = viewModel.editingEventId.value
            Log.d(TAG, "editingEventId = $editingEventId")

            if (editingEventId != null) {
                Log.d(TAG, "Calling updateEvent with id=$editingEventId, text=$text, eventDateTime=$eventDateTime")
                viewModel.updateEvent(
                    editingEventId, text, viewModel.attachment.value, viewModel.coordinates.value,
                    viewModel.eventType.value ?: EventType.OFFLINE, eventDateTime,
                    viewModel.speakerIds.value // Передаем спикеров
                )
            } else {
                Log.e(TAG, "editingEventId is NULL!")
                Toast.makeText(requireContext(), "Ошибка: ID события не найден", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Creating new event")
            viewModel.saveEvent(
                text, viewModel.attachment.value, viewModel.coordinates.value,
                viewModel.eventType.value ?: EventType.OFFLINE, eventDateTime,
                viewModel.speakerIds.value // Передаем спикеров
            )
        }
    }

    override fun onDestroyView() {
        mapView?.onStop()
        mapView = null
        Glide.with(this).clear(binding.imagePreview)
        Glide.with(this).clear(binding.videoPreview)
        super.onDestroyView()
        _binding = null
    }
}