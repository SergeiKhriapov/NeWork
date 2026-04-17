package ru.netology.nework.viewmodel

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

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    val currentUserId: StateFlow<Long?> = tokenManager.currentUserId

    private val _likers = MutableLiveData<List<User>>(emptyList())
    val likers: LiveData<List<User>> = _likers

    private val _mentioned = MutableLiveData<List<User>>(emptyList())
    val mentioned: LiveData<List<User>> = _mentioned

    private val _likersPreview = MutableLiveData<List<UserPreview>>(emptyList())
    val likersPreview: LiveData<List<UserPreview>> = _likersPreview

    private val _mentionedPreview = MutableLiveData<List<UserPreview>>(emptyList())
    val mentionedPreview: LiveData<List<UserPreview>> = _mentionedPreview

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            val result = postRepository.getPostById(postId)
            result.onSuccess { post ->
                _post.value = post
                combineData(post)
            }.onFailure { error ->
                _post.value = null
            }
        }
    }

    fun deletePost() {
        val currentPost = _post.value ?: return
        viewModelScope.launch {
            val result = postRepository.deletePost(currentPost.id)
            result.onFailure { error -> }
        }
    }

    private fun combineData(post: Post) {
        val usersMap = post.users ?: emptyMap()

        val likersList = post.likeOwnerIds.mapNotNull { id ->
            usersMap[id]?.let { preview ->
                User(
                    id = id,
                    login = "",
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

        val likersPreviewList = post.likeOwnerIds.mapNotNull { usersMap[it] }
        val mentionedPreviewList = post.mentionIds.mapNotNull { usersMap[it] }

        _likers.value = likersList
        _mentioned.value = mentionedList
        _likersPreview.value = likersPreviewList
        _mentionedPreview.value = mentionedPreviewList
    }
}