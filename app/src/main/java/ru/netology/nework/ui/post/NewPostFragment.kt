package ru.netology.nework.ui.post

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.app.OnPostActionListener
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.model.MediaAttachment
import ru.netology.nework.model.MediaType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "NewPostFragment"

@AndroidEntryPoint
class NewPostFragment : Fragment(), OnPostActionListener {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewPostViewModel by viewModels()

    // Для редактирования
    private var isEditing = false
    private var editingPostId: Long? = null

    // Для фото с камеры
    private var currentPhotoPath: String? = null

    // Лаунчеры для результатов
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            Log.d(TAG, "cameraLauncher success=$success, currentPhotoPath=$currentPhotoPath")
            if (success && currentPhotoPath != null) {
                val file = File(currentPhotoPath!!)
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                handleMediaResult(uri, MediaType.IMAGE)
            } else {
                Toast.makeText(requireContext(), "Не удалось сделать фото", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.d(TAG, "galleryImageLauncher uri=$uri")
            uri?.let { handleMediaResult(it, MediaType.IMAGE) }
        }

    private val galleryVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.d(TAG, "galleryVideoLauncher uri=$uri")
            uri?.let { handleMediaResult(it, MediaType.VIDEO) }
        }

    private val audioLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.d(TAG, "audioLauncher uri=$uri")
            uri?.let { handleMediaResult(it, MediaType.AUDIO) }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestPermissionLauncher isGranted=$isGranted")
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Необходимо разрешение на камеру",
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

        // Получаем аргументы (для редактирования)
        arguments?.let { args ->
            editingPostId = args.getLong("postId")
            isEditing = editingPostId != null
            if (isEditing) {
                val content = args.getString("content", "")
                binding.editTextPost.setText(content)
                val attachmentUrl = args.getString("attachmentUrl")
                val attachmentType = args.getString("attachmentType")?.let { MediaType.valueOf(it) }
                if (attachmentUrl != null && attachmentType != null) {
                    // Здесь можно показать превью существующего вложения
                    // (например, загрузить изображение по URL)
                    // Для упрощения оставим заглушку
                }
            }
        }

        // Подписка на изменения текста
        binding.editTextPost.doAfterTextChanged { text ->
            viewModel.setText(text.toString())
        }

        // Наблюдение за вложением (с distinctUntilChanged)
        viewModel.attachment
            .distinctUntilChanged()
            .observe(viewLifecycleOwner) { attachment ->
                updateMediaPreview(attachment)
            }

        // Наблюдение за состоянием сохранения
        viewModel.saveCompleted.observe(viewLifecycleOwner) { success ->
            if (success != null && isAdded) {
                when (success) {
                    true -> {
                        Toast.makeText(
                            requireContext(),
                            if (isEditing) "Пост обновлён" else "Пост сохранён",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(100)
                            if (isAdded) findNavController().navigateUp()
                        }
                    }
                    false -> {
                        Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
                viewModel.resetSaveCompleted()
            }
        }

        // Обработка нажатий на иконки в BottomAppBar
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
                    // Упомянуть пользователей (пока заглушка)
                    true
                }
                R.id.action_location -> {
                    // Добавить местоположение (пока заглушка)
                    true
                }
                else -> false
            }
        }

        // Кнопка удаления медиа
        binding.btnRemove.setOnClickListener {
            viewModel.setAttachment(null)
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Camera permission already granted")
                openCamera()
            }
            else -> {
                Log.d(TAG, "Requesting camera permission")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile == null) {
                Log.e(TAG, "Failed to create image file")
                Toast.makeText(requireContext(), "Не удалось создать файл для фото", Toast.LENGTH_SHORT).show()
                return
            }
            currentPhotoPath = photoFile.absolutePath
            Log.d(TAG, "openCamera: photoFile created at $currentPhotoPath")
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(photoUri)
        } else {
            Log.e(TAG, "openCamera: no camera app found")
            Toast.makeText(requireContext(), "Камера не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir("Pictures")
            if (storageDir == null) {
                Log.e(TAG, "External files dir for Pictures is null")
                return null
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file", e)
            null
        }
    }

    private fun showAttachmentDialog() {
        val options = arrayOf("Изображение", "Видео", "Аудио")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Выберите тип вложения")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryImageLauncher.launch("image/*")
                    1 -> galleryVideoLauncher.launch("video/*")
                    2 -> audioLauncher.launch("audio/*")
                }
            }
            .show()
    }

    private fun handleMediaResult(uri: Uri, type: MediaType) {
        Log.d(TAG, "handleMediaResult uri=$uri type=$type")
        // Асинхронное копирование файла
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val internalPath = copyFileToInternalStorage(uri)
            withContext(Dispatchers.Main) {
                if (internalPath == null) {
                    Toast.makeText(requireContext(), "Не удалось скопировать файл", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                Log.d(TAG, "File copied to $internalPath")
                val attachment = MediaAttachment(internalPath, type)
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
            Log.e(TAG, "Error copying file", e)
            null
        }
    }

    private fun updateMediaPreview(attachment: MediaAttachment?) {
        Log.d(TAG, "updateMediaPreview attachment=$attachment")
        // Отменяем предыдущие загрузки Glide
        Glide.with(this).clear(binding.imagePreview)
        Glide.with(this).clear(binding.videoPreview)

        if (attachment == null) {
            binding.imageContainer.visibility = View.GONE
            return
        }

        binding.imageContainer.visibility = View.VISIBLE
        binding.btnRemove.visibility = View.VISIBLE

        // Скрываем все элементы предпросмотра
        binding.imagePreview.visibility = View.GONE
        binding.videoContainer.visibility = View.GONE
        binding.audioPlayer.visibility = View.GONE

        val file = File(attachment.uri)
        if (!file.exists()) {
            Log.e(TAG, "File not found: ${attachment.uri}")
            Toast.makeText(requireContext(), "Файл не найден", Toast.LENGTH_SHORT).show()
            viewModel.setAttachment(null)
            return
        }

        when (attachment.type) {
            MediaType.IMAGE -> {
                binding.imagePreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(file)
                    .centerCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.imagePreview)
            }

            MediaType.VIDEO -> {
                binding.videoContainer.visibility = View.VISIBLE
                // Асинхронное создание миниатюры
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = ThumbnailUtils.createVideoThumbnail(
                        file.absolutePath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            binding.videoPreview.setImageBitmap(bitmap)
                        } else {
                            binding.videoPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    }
                }
            }

            MediaType.AUDIO -> {
                binding.audioPlayer.visibility = View.VISIBLE
                binding.btnPlayPause.setOnClickListener {
                    Toast.makeText(requireContext(), "Воспроизведение аудио", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPostAction() {
        val text = binding.editTextPost.text.toString()
        val attachment = viewModel.attachment.value
        if (isEditing) {
            editingPostId?.let { postId ->
                viewModel.updatePost(postId, text, attachment)
            }
        } else {
            viewModel.savePost()
        }
    }

    override fun onDestroyView() {
        Glide.with(this).clear(binding.imagePreview)
        Glide.with(this).clear(binding.videoPreview)
        super.onDestroyView()
        _binding = null
    }
}