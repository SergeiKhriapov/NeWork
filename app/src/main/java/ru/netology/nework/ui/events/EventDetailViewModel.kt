package ru.netology.nework.viewmodel

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

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _speakers = MutableLiveData<List<User>>()
    val speakers: LiveData<List<User>> = _speakers

    private val _speakersPreview = MutableLiveData<List<UserPreview>>()
    val speakersPreview: LiveData<List<UserPreview>> = _speakersPreview

    private val _likers = MutableLiveData<List<User>>()
    val likers: LiveData<List<User>> = _likers

    private val _likersPreview = MutableLiveData<List<UserPreview>>()
    val likersPreview: LiveData<List<UserPreview>> = _likersPreview

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
                        loadSpeakers(event.speakerIds.toList())
                        loadLikers(event.likeOwnerIds.toList())
                        loadParticipants(event.participantsIds.toList())
                    },
                    onFailure = { error ->
                        _event.value = null
                    }
                )
            } catch (e: Exception) {
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
                    null
                }
            }
            _speakers.value = speakersList
            _speakersPreview.value = speakersList.map { UserPreview(it.name, it.avatar) }
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
                    null
                }
            }
            _likers.value = likersList
            _likersPreview.value = likersList.map { UserPreview(it.name, it.avatar) }
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
                    null
                }
            }
            _participants.value = participantsList
            _participantsPreview.value = participantsList.map { UserPreview(it.name, it.avatar) }
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
                    onSuccess = { },
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
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
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
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
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
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
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
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
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
            }
        }
    }
}