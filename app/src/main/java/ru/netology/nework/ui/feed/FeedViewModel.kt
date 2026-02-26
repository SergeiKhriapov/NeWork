package ru.netology.nework.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Post
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _authError = MutableLiveData<String?>(null)
    val authError: LiveData<String?> = _authError

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("FeedViewModel", "Loading posts...")
            val result = postRepository.getPosts()
            _isLoading.value = false
            result.onSuccess { list ->
                Log.d("FeedViewModel", "Loaded ${list.size} posts")
                _posts.value = list
            }.onFailure { exception ->
                Log.e("FeedViewModel", "Error loading posts", exception)
                _error.value = exception.message ?: "Ошибка загрузки"
            }
        }
    }

    fun likePost(postId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            val result = if (isLiked) {
                postRepository.unlikePost(postId)
            } else {
                postRepository.likePost(postId)
            }
            result.onSuccess { updatedPost ->
                // Обновляем список: заменяем пост на полученный с сервера
                _posts.value = _posts.value?.map { if (it.id == updatedPost.id) updatedPost else it }
            }.onFailure { error ->
                when (error.message) {
                    "Нужно авторизоваться" -> {
                        _authError.value = error.message
                    }
                    else -> {
                        _error.value = error.message ?: "Ошибка при обновлении лайка"
                    }
                }
            }
        }
    }

    // Для обратной совместимости с адаптером (который пока вызывает toggleLike)
    fun toggleLike(postId: Long) {
        // Используем текущее состояние поста, чтобы определить, лайкнут ли он
        val post = _posts.value?.find { it.id == postId } ?: return
        likePost(postId, post.likedByMe)
    }

    fun isLoggedIn(): Boolean {
        // В будущем можно проверять токен через TokenManager, пока заглушка
        return false
    }
}