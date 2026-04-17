package ru.netology.nework.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.Coordinates
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _postText = MutableLiveData("")
    val postText: LiveData<String> = _postText

    private val _attachment = MutableLiveData<Attachment?>(null)
    val attachment: LiveData<Attachment?> = _attachment

    private val _coordinates = MutableLiveData<Coordinates?>(null)
    val coordinates: LiveData<Coordinates?> = _coordinates

    private val _mentionIds = MutableLiveData<Set<Long>>(emptySet())
    val mentionIds: LiveData<Set<Long>> = _mentionIds

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _editingPostId = MutableLiveData<Long?>(null)
    val editingPostId: LiveData<Long?> = _editingPostId

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveCompleted = MutableLiveData<Boolean?>(null)
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    private var argumentsLoaded = false

    fun markArgumentsLoaded() {
        argumentsLoaded = true
    }

    fun isArgumentsLoaded(): Boolean = argumentsLoaded

    fun initEditing(postId: Long, content: String, attachment: Attachment?, coords: Coordinates?, mentionIds: Set<Long>) {
        _isEditing.value = true
        _editingPostId.value = postId
        _postText.value = content
        _attachment.value = attachment
        _coordinates.value = coords
        _mentionIds.value = mentionIds
    }

    fun initNew() {
        _isEditing.value = false
        _editingPostId.value = null
        _postText.value = ""
        _attachment.value = null
        _coordinates.value = null
        _mentionIds.value = emptySet()
    }

    fun setText(text: String) {
        _postText.value = text
    }

    fun setAttachment(attachment: Attachment?) {
        _attachment.value = attachment
    }

    fun setCoordinates(coordinates: Coordinates?) {
        _coordinates.value = coordinates
    }

    fun setMentionIds(ids: Set<Long>) {
        _mentionIds.value = ids
    }

    fun savePost(text: String, attachment: Attachment?, coords: Coordinates?, mentionIds: Set<Long>?) {
        if (text.isBlank() && attachment == null) {
            _saveCompleted.value = false
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = repository.savePost(text, attachment, coords, mentionIds)
                _saveCompleted.value = result.isSuccess
            } catch (e: Exception) {
                _saveCompleted.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updatePost(id: Long, content: String, attachment: Attachment?, coords: Coordinates?, mentionIds: Set<Long>?) {
        if (content.isBlank() && attachment == null) {
            _saveCompleted.value = false
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = repository.updatePost(id, content, attachment, coords, mentionIds)
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