package ru.netology.nework.ui.auth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentRegisterBinding
import java.io.File

private const val TAG = "RegisterFragment"

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d(TAG, "pickImageLauncher: uri=$uri")
        uri?.let {
            binding.ivAvatar.setImageURI(it)
            val file = uriToFile(requireContext(), it)
            viewModel.setAvatarFile(file)
            Log.d(TAG, "Avatar file set: ${file.absolutePath}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        binding.ivAvatar.setOnClickListener {
            Log.d(TAG, "ivAvatar clicked")
            pickImageLauncher.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "btnRegister clicked")
            val login = binding.etLogin.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val confirm = binding.etPasswordConfirm.text.toString().trim()

            Log.d(TAG, "Input values: login=$login, name=$name, password=$password, confirm=$confirm")

            when {
                login.isBlank() -> {
                    Log.d(TAG, "Login is blank")
                    Toast.makeText(requireContext(), "Введите логин", Toast.LENGTH_SHORT).show()
                }
                password.isBlank() -> {
                    Log.d(TAG, "Password is blank")
                    Toast.makeText(requireContext(), "Введите пароль", Toast.LENGTH_SHORT).show()
                }
                name.isBlank() -> {
                    Log.d(TAG, "Name is blank")
                    Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                }
                password != confirm -> {
                    Log.d(TAG, "Passwords do not match")
                    Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.d(TAG, "Calling viewModel.register")
                    viewModel.register(login, password, name)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            Log.d(TAG, "Collecting registerResult")
            viewModel.registerResult.collectLatest { result ->
                Log.d(TAG, "registerResult received: $result")
                result.onSuccess { user ->
                    Log.d(TAG, "Registration success: ${user.name}")
                    Toast.makeText(requireContext(), "Добро пожаловать, ${user.name}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.feedFragment)
                }.onFailure { error ->
                    Log.e(TAG, "Registration failure", error)
                    Toast.makeText(requireContext(), error.message ?: "Ошибка регистрации", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    private fun uriToFile(context: android.content.Context, uri: Uri): File {
        Log.d(TAG, "uriToFile: $uri")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open stream")
        val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        Log.d(TAG, "File created: ${file.absolutePath}")
        return file
    }
}