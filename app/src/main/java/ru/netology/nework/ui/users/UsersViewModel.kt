package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.User
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _selectedUserIds = MutableLiveData<Set<Long>>(emptySet())
    val selectedUserIds: LiveData<Set<Long>> = _selectedUserIds

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getUsers()
            _isLoading.value = false
            result.onSuccess { list ->
                _users.value = list.sortedByDescending { it.id }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to load users"
            }
        }
    }

    fun updateSelectedUsers(ids: Set<Long>) {
        _selectedUserIds.value = ids
    }

    fun getSelectedUserIds(): Set<Long> = _selectedUserIds.value ?: emptySet()
}