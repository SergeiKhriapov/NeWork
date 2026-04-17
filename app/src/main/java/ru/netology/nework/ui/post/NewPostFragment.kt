package ru.netology.nework.ui.post

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
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.User
import ru.netology.nework.ui.users.REQUEST_KEY
import ru.netology.nework.ui.users.SELECTED_USERS_KEY
import ru.netology.nework.utils.LetterAvatarDrawable
import ru.netology.nework.viewmodel.UsersViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOCATION_REQUEST_KEY = "location_request"

@AndroidEntryPoint
class NewPostFragment : Fragment(), OnPostActionListener {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewPostViewModel by viewModels()
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
                Toast.makeText(requireContext(), "Failed to take photo", Toast.LENGTH_SHORT)
                    .show()
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
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideFab()

        if (!viewModel.isArgumentsLoaded()) {
            loadArguments()
            viewModel.markArgumentsLoaded()
        }

        setupObservers()
        setupListeners()
        setupResultListeners()
    }

    private fun loadArguments() {
        arguments?.let { args ->
            val postId = args.getLong("postId", -1)

            if (postId != -1L) {
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

                val mentionIds = args.getLongArray("mentionIds")?.toSet() ?: emptySet()

                viewModel.initEditing(postId, content, attachment, coords, mentionIds)
            } else {
                viewModel.initNew()
            }
        }
    }

    private fun setupObservers() {
        viewModel.postText.observe(viewLifecycleOwner) { text ->
            if (binding.editTextPost.text.toString() != text) {
                binding.editTextPost.setText(text)
            }
        }

        binding.editTextPost.doAfterTextChanged { text ->
            val newText = text?.toString() ?: ""
            if (viewModel.postText.value != newText) {
                viewModel.setText(newText)
            }
        }

        viewModel.attachment
            .distinctUntilChanged()
            .observe(viewLifecycleOwner) { attachment ->
                updateMediaPreview(attachment)
            }

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

        viewModel.mentionIds.observe(viewLifecycleOwner) {
            updateSelectedUsers()
        }

        usersViewModel.users.observe(viewLifecycleOwner) {
            updateSelectedUsers()
        }

        viewModel.isEditing.observe(viewLifecycleOwner) { isEditing ->
            val title = if (isEditing) "Edit post" else "New post"
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title =
                title
        }

        viewModel.saveCompleted.observe(viewLifecycleOwner) { success ->
            if (success != null && isAdded) {
                val isEditing = viewModel.isEditing.value == true
                when (success) {
                    true -> {
                        Toast.makeText(
                            requireContext(),
                            if (isEditing) "Post updated" else "Post saved",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(100)
                            if (isAdded) findNavController().navigateUp()
                        }
                    }

                    false -> {
                        Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
                    }
                }
                viewModel.resetSaveCompleted()
            }
        }
    }

    private fun setupListeners() {
        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_camera -> {
                    checkCameraPermissionAndOpen()
                    true
                }

                R.id.action_attach -> {
                    showAttachmentDialog()
                    true
                }

                R.id.action_users -> {
                    openUserSelection()
                    true
                }

                R.id.action_location -> {
                    openLocationPicker()
                    true
                }

                else -> false
            }
        }

        binding.btnRemove.setOnClickListener {
            viewModel.setAttachment(null)
        }

        binding.btnRemoveLocation.setOnClickListener {
            viewModel.setCoordinates(null)
            Toast.makeText(requireContext(), "Location removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupResultListeners() {
        setFragmentResultListener(LOCATION_REQUEST_KEY) { _, bundle ->
            if (bundle.containsKey("lat") && bundle.containsKey("lng")) {
                val lat = bundle.getDouble("lat")
                val lng = bundle.getDouble("lng")

                if (lat != 0.0 && lng != 0.0) {
                    viewModel.setCoordinates(Coordinates(lat, lng))
                    Toast.makeText(requireContext(), "Location selected", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.setCoordinates(null)
                    Toast.makeText(requireContext(), "Location removed", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                viewModel.setCoordinates(null)
                Toast.makeText(requireContext(), "Location removed", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        setFragmentResultListener(REQUEST_KEY) { _, bundle ->
            val selectedIds = bundle.getLongArray(SELECTED_USERS_KEY)?.toSet() ?: emptySet()
            viewModel.setMentionIds(selectedIds)
            val count = selectedIds.size
            val message = when (count) {
                0 -> "No users selected"
                1 -> "1 user selected"
                else -> "$count users selected"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        hideFab()
    }

    override fun onPause() {
        super.onPause()
        showFab()
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

    private fun hideFab() {
        try {
            requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                R.id.fab_create
            )?.hide()
        } catch (e: Exception) {
        }
    }

    private fun showFab() {
        try {
            requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                R.id.fab_create
            )?.show()
        } catch (e: Exception) {
        }
    }

    private fun openLocationPicker() {
        findNavController().navigate(R.id.locationPickerFragment)
    }

    private fun openUserSelection() {
        val selectedIds = viewModel.mentionIds.value?.toLongArray() ?: longArrayOf()
        val args = Bundle().apply {
            putLongArray("selected_ids", selectedIds)
            putString("title", "Select mentioned")
        }
        findNavController().navigate(R.id.userSelectionFragment, args)
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile == null) {
                Toast.makeText(
                    requireContext(),
                    "Failed to create image file",
                    Toast.LENGTH_SHORT
                ).show()
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
            val storageDir = requireContext().getExternalFilesDir("Pictures")
            if (storageDir == null) {
                return null
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun showAttachmentDialog() {
        val options = arrayOf("Image", "Video", "Audio")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select attachment type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryImageLauncher.launch("image/*")
                    1 -> galleryVideoLauncher.launch("video/*")
                    2 -> audioLauncher.launch("audio/*")
                }
            }
            .show()
    }

    private fun handleMediaResult(uri: Uri, type: AttachmentType) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val internalPath = copyFileToInternalStorage(uri)
            withContext(Dispatchers.Main) {
                if (internalPath == null) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to copy file",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }
                val attachment = Attachment(internalPath, type)
                viewModel.setAttachment(attachment)
            }
        }
    }

    private fun copyFileToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().filesDir, "media_${System.currentTimeMillis()}")
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
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

        val url = attachment.url
        val isLocalFile = !url.startsWith("http://") && !url.startsWith("https://")

        when (attachment.type) {
            AttachmentType.IMAGE -> {
                binding.imagePreview.visibility = View.VISIBLE

                if (isLocalFile) {
                    val file = File(url)
                    if (!file.exists()) {
                        Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT)
                            .show()
                        viewModel.setAttachment(null)
                        return
                    }
                    Glide.with(this)
                        .load(file)
                        .centerCrop()
                        .into(binding.imagePreview)
                } else {
                    Glide.with(this)
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .into(binding.imagePreview)
                }
            }

            AttachmentType.VIDEO -> {
                binding.videoContainer.visibility = View.VISIBLE

                if (isLocalFile) {
                    val file = File(url)
                    if (!file.exists()) {
                        Toast.makeText(requireContext(), "Video not found", Toast.LENGTH_SHORT)
                            .show()
                        viewModel.setAttachment(null)
                        return
                    }
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val bitmap = ThumbnailUtils.createVideoThumbnail(
                            file.absolutePath,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                binding.videoPreview.setImageBitmap(bitmap)
                            } else {
                                binding.videoPreview.setImageResource(R.drawable.ic_play_circle_filled)
                            }
                        }
                    }
                } else {
                    Glide.with(this)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .frame(1000000)
                        .centerCrop()
                        .placeholder(R.drawable.ic_play_circle_filled)
                        .error(R.drawable.ic_play_circle_filled)
                        .into(binding.videoPreview)
                }

                binding.ivPlay.visibility = View.VISIBLE
                binding.videoContainer.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Video playback is not supported yet",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            AttachmentType.AUDIO -> {
                binding.audioPlayer.visibility = View.VISIBLE
                binding.btnPlayPause.setOnClickListener {
                    Toast.makeText(requireContext(), "Audio playback", Toast.LENGTH_SHORT)
                        .show()
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

            val imageProvider = ImageProvider.fromBitmap(bitmap)
            val placemark = mapView?.map?.mapObjects?.addPlacemark(point, imageProvider)
            placemark?.setOpacity(1.0f)

        } catch (e: Exception) {
            val fallbackPlacemark = mapView?.map?.mapObjects?.addPlacemark(point)
            fallbackPlacemark?.setOpacity(1.0f)
        }
    }

    private fun updateSelectedUsers() {
        val allUsers = usersViewModel.users.value ?: return
        val selectedIds = viewModel.mentionIds.value ?: emptySet()
        val selectedUsers = allUsers.filter { it.id in selectedIds }

        if (selectedUsers.isEmpty()) {
            binding.llSelectedUsers.visibility = View.GONE
            binding.tvMentionedLabel.visibility = View.GONE
            return
        }

        binding.llSelectedUsers.visibility = View.VISIBLE
        binding.tvMentionedLabel.visibility = View.VISIBLE
        binding.llSelectedUsers.removeAllViews()

        val avatarSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        val overlap = resources.getDimensionPixelSize(R.dimen.avatar_overlap)
        val iconMarginEnd = resources.getDimensionPixelSize(R.dimen.mention_icon_margin)
        val countMarginEnd = resources.getDimensionPixelSize(R.dimen.mention_count_margin)

        val iconView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_mentioned)
            layoutParams = ViewGroup.MarginLayoutParams(avatarSize, avatarSize).apply {
                marginEnd = iconMarginEnd
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        binding.llSelectedUsers.addView(iconView)

        val countView = TextView(requireContext()).apply {
            text = selectedUsers.size.toString()
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

        val visibleCount = minOf(selectedUsers.size, 5)
        val extraCount = selectedUsers.size - 5

        for (i in 0 until visibleCount) {
            val user = selectedUsers[i]
            val avatarView = createAvatarView(user)
            val layoutParams = avatarView.layoutParams as ViewGroup.MarginLayoutParams
            if (i > 0) {
                layoutParams.marginStart = overlap
            }
            avatarView.layoutParams = layoutParams
            binding.llSelectedUsers.addView(avatarView)
        }

        if (extraCount > 0) {
            val plusButton = createPlusButton {
                openUserSelection()
            }
            val layoutParams = plusButton.layoutParams as ViewGroup.MarginLayoutParams
            if (visibleCount > 0) {
                layoutParams.marginStart = overlap
            }
            plusButton.layoutParams = layoutParams
            binding.llSelectedUsers.addView(plusButton)
        }
    }

    private fun createAvatarView(user: User): View {
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
                showRemoveUserDialog(user)
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
                Glide.with(this@NewPostFragment)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(this)
            } else {
                val firstLetter = user.name.firstOrNull()?.toString() ?: "?"
                val drawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(
                        requireContext(),
                        R.color.purple_primary
                    )
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

    private fun showRemoveUserDialog(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove user")
            .setMessage("Remove ${user.name} from mentioned?")
            .setPositiveButton("Yes") { _, _ ->
                val currentIds =
                    viewModel.mentionIds.value?.toMutableSet() ?: return@setPositiveButton
                currentIds.remove(user.id)
                viewModel.setMentionIds(currentIds)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onPostAction() {
        if (viewModel.isSaving.value == true) {
            Toast.makeText(requireContext(), "Saving in progress", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val text = binding.editTextPost.text.toString()
        val attachment = viewModel.attachment.value
        val coordinates = viewModel.coordinates.value
        val mentionIds = viewModel.mentionIds.value

        val isEditing = viewModel.isEditing.value == true
        if (isEditing) {
            val editingPostId = viewModel.editingPostId.value
            editingPostId?.let { postId ->
                viewModel.updatePost(postId, text, attachment, coordinates, mentionIds)
            }
        } else {
            viewModel.savePost(text, attachment, coordinates, mentionIds)
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