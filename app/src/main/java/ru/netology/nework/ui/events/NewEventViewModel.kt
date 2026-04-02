package ru.netology.nework.ui.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.EventRepository
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.EventType
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "NewEventViewModel"

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _eventText = MutableLiveData("")
    val eventText: LiveData<String> = _eventText

    private val _attachment = MutableLiveData<Attachment?>(null)
    val attachment: LiveData<Attachment?> = _attachment

    private val _coordinates = MutableLiveData<Coordinates?>(null)
    val coordinates: LiveData<Coordinates?> = _coordinates

    private val _participantIds = MutableLiveData<Set<Long>>(emptySet())
    val participantIds: LiveData<Set<Long>> = _participantIds

    private val _eventType = MutableLiveData(EventType.OFFLINE)
    val eventType: LiveData<EventType> = _eventType

    private val _eventDateTime = MutableLiveData<LocalDateTime?>(null)
    val eventDateTime: LiveData<LocalDateTime?> = _eventDateTime

    private val _isEditing = MutableLiveData(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _editingEventId = MutableLiveData<Long?>(null)
    val editingEventId: LiveData<Long?> = _editingEventId

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveCompleted = MutableLiveData<Boolean?>(null)
    val saveCompleted: LiveData<Boolean?> = _saveCompleted

    private var argumentsLoaded = false

    fun markArgumentsLoaded() {
        argumentsLoaded = true
    }

    fun isArgumentsLoaded(): Boolean = argumentsLoaded

    fun initEditing(
        eventId: Long,
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime?,
        participantIds: Set<Long>
    ) {
        Log.d(TAG, "initEditing: eventId=$eventId, eventDateTime=$eventDateTime")
        _isEditing.value = true
        _editingEventId.value = eventId
        _eventText.value = content
        _attachment.value = attachment
        _coordinates.value = coords
        _eventType.value = eventType
        _eventDateTime.value = eventDateTime
        _participantIds.value = participantIds
    }

    fun initNew() {
        Log.d(TAG, "initNew")
        _isEditing.value = false
        _editingEventId.value = null
        _eventText.value = ""
        _attachment.value = null
        _coordinates.value = null
        _eventType.value = EventType.OFFLINE
        _eventDateTime.value = null
        _participantIds.value = emptySet()
    }

    fun setText(text: String) {
        _eventText.value = text
    }

    fun setAttachment(attachment: Attachment?) {
        _attachment.value = attachment
    }

    fun setCoordinates(coordinates: Coordinates?) {
        _coordinates.value = coordinates
    }

    fun setParticipantIds(ids: Set<Long>) {
        _participantIds.value = ids
    }

    fun setEventType(type: EventType) {
        _eventType.value = type
    }

    fun setEventDateTime(dateTime: LocalDateTime?) {
        _eventDateTime.value = dateTime
    }

    fun saveEvent(
        text: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime?,
        participantIds: Set<Long>?
    ) {
        Log.d(TAG, "saveEvent: text=$text, eventType=${eventType.name}, eventDateTime=$eventDateTime")

        if (text.isBlank() && attachment == null) {
            Log.e(TAG, "saveEvent: text is blank and attachment is null")
            _saveCompleted.value = false
            return
        }
        if (eventDateTime == null) {
            Log.e(TAG, "saveEvent: eventDateTime is null!")
            _saveCompleted.value = false
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                Log.d(TAG, "Calling repository.saveEvent")
                val result = repository.saveEvent(text, attachment, coords, eventType, eventDateTime, participantIds ?: emptySet())
                Log.d(TAG, "repository.saveEvent result.isSuccess = ${result.isSuccess}")
                _saveCompleted.value = result.isSuccess
            } catch (e: Exception) {
                Log.e(TAG, "saveEvent exception", e)
                _saveCompleted.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateEvent(
        id: Long,
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime?,
        participantIds: Set<Long>?
    ) {
        Log.d(TAG, "=== updateEvent IN VIEWMODEL ===")
        Log.d(TAG, "updateEvent: id=$id")
        Log.d(TAG, "updateEvent: content=$content")
        Log.d(TAG, "updateEvent: eventType=${eventType.name}")
        Log.d(TAG, "updateEvent: eventDateTime=$eventDateTime")
        Log.d(TAG, "updateEvent: participantIds=${participantIds?.joinToString()}")

        if (content.isBlank() && attachment == null) {
            Log.e(TAG, "updateEvent: content is blank and attachment is null")
            _saveCompleted.value = false
            return
        }
        if (eventDateTime == null) {
            Log.e(TAG, "updateEvent: eventDateTime is null!")
            _saveCompleted.value = false
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                Log.d(TAG, "Calling repository.updateEvent")
                val result = repository.updateEvent(id, content, attachment, coords, eventType, eventDateTime, participantIds ?: emptySet())
                Log.d(TAG, "repository.updateEvent result.isSuccess = ${result.isSuccess}")
                _saveCompleted.value = result.isSuccess
            } catch (e: Exception) {
                Log.e(TAG, "updateEvent exception", e)
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