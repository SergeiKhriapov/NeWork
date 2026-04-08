package ru.netology.nework.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.EventRepository
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.Event
import ru.netology.nework.model.User
import ru.netology.nework.model.UserPreview
import javax.inject.Inject

private const val TAG = "EventDetailViewModel"

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    // Speakers - спикеры (те, кого выбрали при создании)
    private val _speakers = MutableLiveData<List<User>>()
    val speakers: LiveData<List<User>> = _speakers

    private val _speakersPreview = MutableLiveData<List<UserPreview>>()
    val speakersPreview: LiveData<List<UserPreview>> = _speakersPreview

    // Likers - лайкнувшие
    private val _likers = MutableLiveData<List<User>>()
    val likers: LiveData<List<User>> = _likers

    private val _likersPreview = MutableLiveData<List<UserPreview>>()
    val likersPreview: LiveData<List<UserPreview>> = _likersPreview

    // Participants - участники (те, кто нажал "Участвовать")
    private val _participants = MutableLiveData<List<User>>()
    val participants: LiveData<List<User>> = _participants

    private val _participantsPreview = MutableLiveData<List<UserPreview>>()
    val participantsPreview: LiveData<List<UserPreview>> = _participantsPreview

    private val _currentUserId = MutableLiveData<Long?>()
    val currentUserId: LiveData<Long?> = _currentUserId

    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val result = eventRepository.getEventById(eventId)
                result.fold(
                    onSuccess = { event ->
                        _event.value = event
                        Log.d(TAG, "Event loaded: id=${event.id}, speakerIds=${event.speakerIds}, participantsIds=${event.participantsIds}")

                        // Загружаем спикеров (те, кого выбрали при создании)
                        loadSpeakers(event.speakerIds.toList())

                        // Загружаем лайкнувших
                        loadLikers(event.likeOwnerIds.toList())

                        // Загружаем участников (те, кто нажал "Участвовать")
                        loadParticipants(event.participantsIds.toList())
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading event: ${error.message}")
                        _event.value = null
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading event", e)
                _event.value = null
            }
        }
    }

    private suspend fun loadSpeakers(speakerIds: List<Long>) {
        if (speakerIds.isNotEmpty()) {
            val speakersList = speakerIds.mapNotNull { userId ->
                try {
                    userRepository.getUserById(userId).getOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading speaker $userId", e)
                    null
                }
            }
            _speakers.value = speakersList
            _speakersPreview.value = speakersList.map { UserPreview(it.name, it.avatar) }
            Log.d(TAG, "Speakers loaded: ${speakersList.size}")
        } else {
            _speakers.value = emptyList()
            _speakersPreview.value = emptyList()
        }
    }

    private suspend fun loadLikers(likeOwnerIds: List<Long>) {
        if (likeOwnerIds.isNotEmpty()) {
            val likersList = likeOwnerIds.mapNotNull { userId ->
                try {
                    userRepository.getUserById(userId).getOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading liker $userId", e)
                    null
                }
            }
            _likers.value = likersList
            _likersPreview.value = likersList.map { UserPreview(it.name, it.avatar) }
            Log.d(TAG, "Likers loaded: ${likersList.size}")
        } else {
            _likers.value = emptyList()
            _likersPreview.value = emptyList()
        }
    }

    private suspend fun loadParticipants(participantIds: List<Long>) {
        if (participantIds.isNotEmpty()) {
            val participantsList = participantIds.mapNotNull { userId ->
                try {
                    userRepository.getUserById(userId).getOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading participant $userId", e)
                    null
                }
            }
            _participants.value = participantsList
            _participantsPreview.value = participantsList.map { UserPreview(it.name, it.avatar) }
            Log.d(TAG, "Participants loaded: ${participantsList.size}")
        } else {
            _participants.value = emptyList()
            _participantsPreview.value = emptyList()
        }
    }

    fun deleteEvent() {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(currentEvent.id).fold(
                    onSuccess = {
                        Log.d(TAG, "Event deleted: ${currentEvent.id}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error deleting event: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting event", e)
            }
        }
    }

    fun setCurrentUserId(userId: Long) {
        _currentUserId.value = userId
    }

    fun likeEvent() {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            try {
                eventRepository.likeEvent(currentEvent.id).fold(
                    onSuccess = { updatedEvent ->
                        _event.value = updatedEvent
                        loadLikers(updatedEvent.likeOwnerIds.toList())
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error liking event: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error liking event", e)
            }
        }
    }

    fun unlikeEvent() {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            try {
                eventRepository.unlikeEvent(currentEvent.id).fold(
                    onSuccess = { updatedEvent ->
                        _event.value = updatedEvent
                        loadLikers(updatedEvent.likeOwnerIds.toList())
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error unliking event: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error unliking event", e)
            }
        }
    }

    fun participateEvent() {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            try {
                eventRepository.participateEvent(currentEvent.id).fold(
                    onSuccess = { updatedEvent ->
                        _event.value = updatedEvent
                        loadParticipants(updatedEvent.participantsIds.toList())
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error participating in event: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error participating in event", e)
            }
        }
    }

    fun unparticipateEvent() {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            try {
                eventRepository.unparticipateEvent(currentEvent.id).fold(
                    onSuccess = { updatedEvent ->
                        _event.value = updatedEvent
                        loadParticipants(updatedEvent.participantsIds.toList())
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error unparticipating from event: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error unparticipating from event", e)
            }
        }
    }
}