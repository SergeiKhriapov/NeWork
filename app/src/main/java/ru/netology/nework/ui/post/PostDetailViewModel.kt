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
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import ru.netology.nework.model.UserPreview
import javax.inject.Inject

private const val TAG = "PostDetailVM"

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val tokenManager: TokenManager  // Добавляем TokenManager
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    // Добавляем currentUserId
    val currentUserId: StateFlow<Long?> = tokenManager.currentUserId

    // Используем User с реальными ID
    private val _likers = MutableLiveData<List<User>>(emptyList())
    val likers: LiveData<List<User>> = _likers

    private val _mentioned = MutableLiveData<List<User>>(emptyList())
    val mentioned: LiveData<List<User>> = _mentioned

    // Для карусели может понадобиться UserPreview (если нужно отдельно)
    private val _likersPreview = MutableLiveData<List<UserPreview>>(emptyList())
    val likersPreview: LiveData<List<UserPreview>> = _likersPreview

    private val _mentionedPreview = MutableLiveData<List<UserPreview>>(emptyList())
    val mentionedPreview: LiveData<List<UserPreview>> = _mentionedPreview

    fun loadPost(postId: Long) {
        Log.d(TAG, "loadPost($postId) called")
        viewModelScope.launch {
            Log.d(TAG, "Calling postRepository.getPostById($postId)")
            val result = postRepository.getPostById(postId)
            result.onSuccess { post ->
                Log.d(TAG, "Post loaded successfully: id=${post.id}, author=${post.author}")
                _post.value = post
                combineData(post)
            }.onFailure { error ->
                Log.e(TAG, "Error loading post: ${error.message}")
                _post.value = null
            }
        }
    }

    fun deletePost() {
        val currentPost = _post.value ?: return
        viewModelScope.launch {
            val result = postRepository.deletePost(currentPost.id)
            result.onFailure { error ->
                Log.e(TAG, "Error deleting post: ${error.message}")
            }
        }
    }

    private fun combineData(post: Post) {
        val usersMap = post.users ?: emptyMap()
        Log.d(TAG, "combineData: likeOwnerIds=${post.likeOwnerIds}, mentionIds=${post.mentionIds}")
        Log.d(TAG, "usersMap size = ${usersMap.size}")

        // Конвертируем UserPreview в User с реальными ID
        val likersList = post.likeOwnerIds.mapNotNull { id ->
            usersMap[id]?.let { preview ->
                User(
                    id = id,
                    login = "", // login нет в UserPreview, можно оставить пустым
                    name = preview.name,
                    avatar = preview.avatar
                )
            }
        }

        val mentionedList = post.mentionIds.mapNotNull { id ->
            usersMap[id]?.let { preview ->
                User(
                    id = id,
                    login = "",
                    name = preview.name,
                    avatar = preview.avatar
                )
            }
        }

        // Для карусели (только имена и аватары)
        val likersPreviewList = post.likeOwnerIds.mapNotNull { usersMap[it] }
        val mentionedPreviewList = post.mentionIds.mapNotNull { usersMap[it] }

        Log.d(TAG, "likers count = ${likersList.size}, mentioned count = ${mentionedList.size}")

        _likers.value = likersList
        _mentioned.value = mentionedList
        _likersPreview.value = likersPreviewList
        _mentionedPreview.value = mentionedPreviewList
    }
}