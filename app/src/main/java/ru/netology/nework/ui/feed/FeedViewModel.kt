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

    fun toggleLike(postId: Long) {
        // Здесь позже можно будет отправить запрос на сервер, а пока локальное обновление
        _posts.value = _posts.value?.map {
            if (it.id == postId) {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likes = if (!it.likedByMe) it.likes + 1 else it.likes - 1
                )
            } else it
        }
    }

    fun isLoggedIn(): Boolean {
        // В будущем можно проверять токен через TokenManager, пока заглушка
        return false
    }
}