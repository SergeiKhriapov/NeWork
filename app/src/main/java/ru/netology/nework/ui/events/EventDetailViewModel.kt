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
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.getEventById(eventId)

            _isLoading.value = false
            result.onSuccess { event ->
                _event.value = event
            }.onFailure { error ->
                _error.value = error.message ?: "Ошибка загрузки события"
            }
        }
    }
}