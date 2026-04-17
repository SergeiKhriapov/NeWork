package ru.netology.nework.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.AuthRepository
import ru.netology.nework.model.User
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerResult = MutableSharedFlow<Result<User>>()
    val registerResult = _registerResult.asSharedFlow()

    private var avatarFile: File? = null

    fun setAvatarFile(file: File?) {
        avatarFile = file
    }

    fun register(login: String, password: String, name: String) {
        viewModelScope.launch {
            val result = authRepository.register(login, password, name, avatarFile)
            _registerResult.emit(result)
        }
    }
}