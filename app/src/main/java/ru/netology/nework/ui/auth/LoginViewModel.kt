package ru.netology.nework.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.AuthRepository
import ru.netology.nework.model.User
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableSharedFlow<Result<User>>()
    val loginResult = _loginResult.asSharedFlow()

    fun login(login: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.login(login, password)
            _loginResult.emit(result)
        }
    }
}