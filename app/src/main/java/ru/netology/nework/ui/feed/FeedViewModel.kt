package ru.netology.nework.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.data.datastore.TokenManager
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Attachment  // Изменён импорт
import ru.netology.nework.model.Post
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    val posts: LiveData<List<Post>> = postRepository.getPostsLiveData()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _authError = MutableLiveData<String?>(null)
    val authError: LiveData<String?> = _authError

    val currentUserId: StateFlow<Long?> = tokenManager.currentUserId

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("FeedViewModel", "Loading posts from network...")
            val result = postRepository.getPosts()
            _isLoading.value = false
            result.onFailure { exception ->
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
            result.onFailure { error ->
                when (error.message) {
                    "Нужно авторизоваться" -> _authError.value = error.message
                    else -> _error.value = error.message ?: "Ошибка при обновлении лайка"
                }
            }
        }
    }

    fun toggleLike(postId: Long) {
        val post = posts.value?.find { it.id == postId } ?: return
        likePost(postId, post.likedByMe)
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            val result = postRepository.deletePost(postId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка удаления"
            }
        }
    }

    // Изменён тип attachment с MediaAttachment? на Attachment?
    fun updatePost(postId: Long, content: String?, attachment: Attachment?) {
        viewModelScope.launch {
            val result = postRepository.updatePost(postId, content, attachment, null, null)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка обновления"
            }
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.token.value != null
}