package ru.netology.nework.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.domain.repository.EventRepository
import ru.netology.nework.model.Event
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    val events: LiveData<List<Event>> = repository.getEventsLiveData()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getEvents()
            _isLoading.value = false
            result.onFailure { exception ->
                _error.value = exception.message ?: "Ошибка загрузки"
            }
        }
    }

    fun likeEvent(eventId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            val result = if (isLiked) {
                repository.unlikeEvent(eventId)
            } else {
                repository.likeEvent(eventId)
            }
            result.onFailure { error ->
                _error.value = error.message ?: "Ошибка при обновлении лайка"
            }
        }
    }

    fun participateEvent(eventId: Long, isParticipating: Boolean) {
        viewModelScope.launch {
            val result = if (isParticipating) {
                repository.unparticipateEvent(eventId)
            } else {
                repository.participateEvent(eventId)
            }
            result.onFailure { error ->
                _error.value = error.message ?: "Ошибка при обновлении участия"
            }
        }
    }
}