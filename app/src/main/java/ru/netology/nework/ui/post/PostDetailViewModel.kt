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
import ru.netology.nework.model.UserPreview
import javax.inject.Inject

private const val TAG = "PostDetailVM"

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    // Используем UserPreview вместо User
    private val _likers = MutableLiveData<List<UserPreview>>(emptyList())
    val likers: LiveData<List<UserPreview>> = _likers

    private val _mentioned = MutableLiveData<List<UserPreview>>(emptyList())
    val mentioned: LiveData<List<UserPreview>> = _mentioned

    fun loadPost(postId: Long) {
        Log.d(TAG, "loadPost($postId) called")
        viewModelScope.launch {
            Log.d(TAG, "Calling postRepository.getPostById($postId)")
            val result = postRepository.getPostById(postId)
            result.onSuccess { post ->
                Log.d(TAG, "Post loaded successfully: id=${post.id}, author=${post.author}")
                _post.value = post
                combineData()
            }.onFailure { error ->
                Log.e(TAG, "Error loading post: ${error.message}")
                _post.value = null
            }
        }
    }

    private fun combineData() {
        val post = _post.value ?: run {
            Log.d(TAG, "combineData: post is null")
            return
        }
        val usersMap = post.users ?: emptyMap()
        Log.d(TAG, "combineData: likeOwnerIds=${post.likeOwnerIds}, mentionIds=${post.mentionIds}")
        Log.d(TAG, "usersMap size = ${usersMap.size}")

        val likersList = post.likeOwnerIds.mapNotNull { usersMap[it] }
        val mentionedList = post.mentionIds.mapNotNull { usersMap[it] }

        Log.d(TAG, "likers count = ${likersList.size}, mentioned count = ${mentionedList.size}")
        _likers.value = likersList
        _mentioned.value = mentionedList
    }
}