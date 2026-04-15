package ru.netology.nework.ui.users

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.Job
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _userDetail = MutableLiveData<UserDetailData?>()
    val userDetail: LiveData<UserDetailData?> = _userDetail

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserDetail(userId: Long, user: User? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = userRepository.getUserDetail(userId, user)

            result.onSuccess { detail ->
                _userDetail.value = UserDetailData(
                    user = detail.user,
                    wallPosts = detail.wallPosts,
                    jobs = detail.jobs
                )
            }.onFailure { exception ->
                _error.value = exception.message ?: "Ошибка загрузки данных пользователя"
            }

            _isLoading.value = false
        }
    }

    fun onLikePost(post: Post) {
        viewModelScope.launch {
            val result = if (post.likedByMe) {
                postRepository.unlikePost(post.id)
            } else {
                postRepository.likePost(post.id)
            }

            result.onSuccess { updatedPost ->
                updatePostInWall(updatedPost)
            }.onFailure { exception ->
                _error.value = exception.message ?: "Ошибка при обновлении лайка"
            }
        }
    }

    fun createJob(job: Job) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.createJob(job)
            result.onSuccess { newJob ->
                val currentData = _userDetail.value
                currentData?.let {
                    val updatedJobs = it.jobs + newJob
                    _userDetail.value = it.copy(jobs = updatedJobs)
                }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Ошибка создания работы"
            }
            _isLoading.value = false
        }
    }

    fun deleteJob(job: Job) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.deleteJob(job.id)
            result.onSuccess {
                val currentData = _userDetail.value
                currentData?.let {
                    val updatedJobs = it.jobs.filter { it.id != job.id }
                    _userDetail.value = it.copy(jobs = updatedJobs)
                }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Ошибка удаления работы"
            }
            _isLoading.value = false
        }
    }

    private fun updatePostInWall(updatedPost: Post) {
        val currentData = _userDetail.value ?: return
        val updatedPosts = currentData.wallPosts.map { post ->
            if (post.id == updatedPost.id) updatedPost else post
        }
        _userDetail.value = currentData.copy(wallPosts = updatedPosts)
    }

    data class UserDetailData(
        val user: User,
        val wallPosts: List<Post>,
        val jobs: List<Job>
    )
}