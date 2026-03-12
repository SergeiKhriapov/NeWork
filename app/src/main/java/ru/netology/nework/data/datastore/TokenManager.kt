package ru.netology.nework.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nework.model.User
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("auth")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
    }

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[TOKEN_KEY] }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.first().let { prefs ->
                _token.value = prefs[TOKEN_KEY]
                // Читаем userId как String и конвертируем в Long?
                _currentUserId.value = prefs[USER_ID_KEY]?.toLongOrNull()
                val name = prefs[USER_NAME_KEY]
                val avatar = prefs[USER_AVATAR_KEY]
                val userId = _currentUserId.value ?: 0L
                if (!name.isNullOrBlank()) {
                    _currentUser.value = User(
                        id = userId,
                        login = "",
                        name = name,
                        avatar = avatar?.takeIf { it.isNotBlank() }
                    )
                }
            }
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
        _token.value = token
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = user.id.toString()
            preferences[USER_NAME_KEY] = user.name
            preferences[USER_AVATAR_KEY] = user.avatar ?: ""
        }
        _currentUserId.value = user.id
        _currentUser.value = user
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(USER_AVATAR_KEY)
        }
        _token.value = null
        _currentUserId.value = null
        _currentUser.value = null
    }

    fun setCurrentUser(user: User) {
        _currentUserId.value = user.id
        _currentUser.value = user
    }
}