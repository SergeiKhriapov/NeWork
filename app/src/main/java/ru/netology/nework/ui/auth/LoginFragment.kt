package ru.netology.nework.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentLoginBinding

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentLoginBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupListeners()
        observeLoginResult()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            handleLoginClick()
        }

        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun handleLoginClick() {
        val login = binding.etLogin.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (login.isBlank() || password.isBlank()) {
            showToast("Please fill in all fields")
            return
        }

        viewModel.login(login, password)
    }

    private fun observeLoginResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginResult.collectLatest { result ->
                    result
                        .onSuccess { user ->
                            showToast("Welcome, ${user.name}")
                            navigateToFeed()
                        }
                        .onFailure { error ->
                            showToast(error.message ?: "Something went wrong")
                        }
                }
            }
        }
    }

    private fun navigateToFeed() {
        findNavController().navigate(R.id.action_loginFragment_to_feedFragment)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}