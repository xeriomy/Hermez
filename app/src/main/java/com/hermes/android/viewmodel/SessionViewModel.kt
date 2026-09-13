// Hermes Android Client - Session ViewModel
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.model.*
import com.hermes.android.network.HermesApi
import com.hermes.android.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing sessions
 */
class SessionViewModel(
    application: Application,
    private val api: HermesApi
) : AndroidViewModel(application) {
    private const val TAG = "SessionViewModel"
    
    // Repository
    private val sessionRepository: SessionRepository = SessionRepository(api)
    
    // Sessions state
    private val _sessionsState = MutableStateFlow<SessionListState>(SessionListState.Idle)
    val sessionsState: StateFlow<SessionListState> = _sessionsState.asStateFlow()
    
    // Selected session
    private val _selectedSessionState = MutableStateFlow<Session?>(null)
    val selectedSessionState: StateFlow<Session?> = _selectedSessionState.asStateFlow()
    
    // Session creation state
    private val _sessionCreationState = MutableStateFlow<SessionCreationState>(SessionCreationState.Idle)
    val sessionCreationState: StateFlow<SessionCreationState> = _sessionCreationState.asStateFlow()
    
    // Error state
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()
    
    init {
        // Observe repository state
        viewModelScope.launch {
            sessionRepository.sessionsState.collect { state ->
                when (state) {
                    is SessionState.Idle -> {
                        _sessionsState.value = SessionListState.Idle
                    }
                    is SessionState.Loading -> {
                        _sessionsState.value = SessionListState.Loading
                    }
                    is SessionState.Success -> {
                        _sessionsState.value = SessionListState.Success(state.sessions)
                    }
                    is SessionState.Error -> {
                        _sessionsState.value = SessionListState.Error(state.message)
                        _errorState.value = state.message
                    }
                }
            }
        }
        
        viewModelScope.launch {
            sessionRepository.sessionCreationState.collect { state ->
                when (state) {
                    is SessionCreationState.Idle -> {
                        _sessionCreationState.value = SessionCreationState.Idle
                    }
                    is SessionCreationState.Loading -> {
                        _sessionCreationState.value = SessionCreationState.Loading
                    }
                    is SessionCreationState.Success -> {
                        _sessionCreationState.value = SessionCreationState.Success(state.response)
                        // Refresh sessions after creation
                        loadSessions()
                    }
                    is SessionCreationState.Error -> {
                        _sessionCreationState.value = SessionCreationState.Error(state.message)
                        _errorState.value = state.message
                    }
                }
            }
        }
    }
    
    /**
     * Load sessions
     */
    fun loadSessions() {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.loadSessions()
        }
    }
    
    /**
     * Create a new session
     */
    fun createSession(title: String, profile: String? = null) {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.createSession(title, profile)
        }
    }
    
    /**
     * Select a session
     */
    fun selectSession(session: Session) {
        _selectedSessionState.value = session
        _errorState.value = null
    }
    
    /**
     * Clear selected session
     */
    fun clearSelectedSession() {
        _selectedSessionState.value = null
    }
    
    /**
     * Refresh sessions
     */
    fun refreshSessions() {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.refreshSessions()
        }
    }
    
    /**
     * Get cached sessions
     */
    fun getCachedSessions(): List<Session> {
        return sessionRepository.getCachedSessions()
    }
    
    /**
     * Get repository for more advanced operations
     */
    fun getRepository(): SessionRepository {
        return sessionRepository
    }
}

/**
 * Session list state for UI
 */
sealed class SessionListState {
    object Idle : SessionListState()
    object Loading : SessionListState()
    data class Success(val sessions: List<Session>) : SessionListState()
    data class Error(val message: String) : SessionListState()
}

/**
 * Session creation state for UI
 */
sealed class SessionCreationState {
    object Idle : SessionCreationState()
    object Loading : SessionCreationState()
    data class Success(val response: CreateSessionResponse) : SessionCreationState()
    data class Error(val message: String) : SessionCreationState()
}
