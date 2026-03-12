package ru.netology.nework.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Attachment  // Изменён импорт
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _postText = MutableLiveData<String>()
    val postText: LiveData<String> = _postText

    // Изменено с AttachmentType? на Attachment?
    private val _attachment = MutableLiveData<Attachment?>(null)
    val attachment: LiveData<Attachment?> = _attachment

    private val _coordinates = MutableLiveData<Coordinates?>(null)
    val coordinates: LiveData<Coordinates?> = _coordinates

    private val _mentionIds = MutableLiveData<Set<Long>>(emptySet())
    val mentionIds: LiveData<Set<Long>> = _mentionIds

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveCompleted = MutableLiveData<Boolean?>(null)
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    fun setText(text: String) {
        _postText.value = text
    }

    // Изменён тип параметра
    fun setAttachment(attachment: Attachment?) {
        _attachment.value = attachment
    }

    fun setCoordinates(coordinates: Coordinates?) {
        _coordinates.value = coordinates
    }

    fun setMentionIds(ids: Set<Long>) {
        _mentionIds.value = ids
    }

    // Изменён тип параметра attachment
    fun savePost(text: String?, attachment: Attachment?, coords: Coordinates?, mentionIds: Set<Long>?) {
        if (text.isNullOrBlank() && attachment == null) {
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

    // Изменён тип параметра attachment
    fun updatePost(id: Long, content: String?, attachment: Attachment?, coords: Coordinates?, mentionIds: Set<Long>?) {
        if (content.isNullOrBlank() && attachment == null) {
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