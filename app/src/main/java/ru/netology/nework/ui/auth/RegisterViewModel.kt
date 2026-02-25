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
import android.util.Log

private const val TAG = "RegisterViewModel"

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerResult = MutableSharedFlow<Result<User>>()
    val registerResult = _registerResult.asSharedFlow()

    private var avatarFile: File? = null

    fun setAvatarFile(file: File?) {
        avatarFile = file
        Log.d(TAG, "setAvatarFile: $file")
    }

    fun register(login: String, password: String, name: String) {
        Log.d(TAG, "register called with login=$login, name=$name")
        viewModelScope.launch {
            Log.d(TAG, "Launching coroutine")
            val result = authRepository.register(login, password, name, avatarFile)
            Log.d(TAG, "repository.register returned: $result")
            _registerResult.emit(result)
            Log.d(TAG, "Result emitted")
        }
    }
}