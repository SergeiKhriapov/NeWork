package ru.netology.nework.ui.auth

import android.net.Uri
import android.os.Bundle
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

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.ivAvatar.setImageURI(it)
            val file = uriToFile(requireContext(), it)
            viewModel.setAvatarFile(file)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val confirm = binding.etPasswordConfirm.text.toString().trim()

            when {
                login.isBlank() -> {
                    Toast.makeText(requireContext(), "Enter login", Toast.LENGTH_SHORT).show()
                }
                password.isBlank() -> {
                    Toast.makeText(requireContext(), "Enter password", Toast.LENGTH_SHORT).show()
                }
                name.isBlank() -> {
                    Toast.makeText(requireContext(), "Enter name", Toast.LENGTH_SHORT).show()
                }
                password != confirm -> {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    viewModel.register(login, password, name)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.registerResult.collectLatest { result ->
                result.onSuccess { user ->
                    Toast.makeText(requireContext(), "Welcome, ${user.name}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.feedFragment)
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "Registration error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun uriToFile(context: android.content.Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open stream")
        val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file
    }
}