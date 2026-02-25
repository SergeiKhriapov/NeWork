package ru.netology.nework.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentLoginBinding

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Кнопка входа
        binding.btnLogin.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (login.isNotBlank() && password.isNotBlank()) {
                viewModel.login(login, password)
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Ссылка на регистрацию (добавлено из второго варианта)
        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        // Наблюдаем за результатом входа
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.loginResult.collectLatest { result ->
                result.onSuccess { user ->
                    Toast.makeText(requireContext(), "Добро пожаловать, ${user.name}", Toast.LENGTH_LONG).show()
                    // Переход на главный экран (FeedFragment)
                    findNavController().navigate(R.id.action_loginFragment_to_feedFragment)
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}