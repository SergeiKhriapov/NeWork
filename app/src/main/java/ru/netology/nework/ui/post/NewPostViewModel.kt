package ru.netology.nework.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.MediaAttachment
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _postText = MutableLiveData<String>()
    val postText: LiveData<String> = _postText

    private val _attachment = MutableLiveData<MediaAttachment?>(null)
    val attachment: LiveData<MediaAttachment?> = _attachment

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveCompleted = MutableLiveData<Boolean?>(null)
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    fun setText(text: String) {
        _postText.value = text
    }

    fun setAttachment(attachment: MediaAttachment?) {
        _attachment.value = attachment
    }

    fun savePost() {
        val text = _postText.value
        val attachment = _attachment.value

        if (text.isNullOrBlank() && attachment == null) {
            _saveCompleted.value = false
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = repository.savePost(text, attachment)
                _saveCompleted.value = result.isSuccess
            } catch (e: Exception) {
                _saveCompleted.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updatePost(id: Long, content: String?, attachment: MediaAttachment?) {
        if (content.isNullOrBlank() && attachment == null) {
            _saveCompleted.value = false
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = repository.updatePost(id, content, attachment)
                _saveCompleted.value = result.isSuccess
            } catch (e: Exception) {
                _saveCompleted.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun resetSaveCompleted() {
        _saveCompleted.value = null
    }
}