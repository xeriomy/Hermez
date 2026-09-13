// Hermes Android Client - Chat Repository
// This file is part of the Hermes Android project

package com.hermes.android.repository

import android.util.Log
import com.hermes.android.model.*
import com.hermes.android.network.HermesApi
import com.hermes.android.network.SseClient
import com.hermes.android.network.SseEventType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for managing chat operations and streaming
 */
class ChatRepository(
    private val api: HermesApi,
    private val sseClient: SseClient
) {
    private const val TAG = "ChatRepository"
    
    // State for chat
    private val _chatState = MutableStateFlow<ChatState>(ChatState.IDLE)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    // State for messages in current session
    private val _messagesState = MutableStateFlow<List<Message>>(emptyList())
    val messagesState: StateFlow<List<Message>> = _messagesState.asStateFlow()
    
    // State for streaming message
    private val _streamingMessageState = MutableStateFlow<String?>(null)
    val streamingMessageState: StateFlow<String?> = _streamingMessageState.asStateFlow()
    
    // State for chat start
    private val _chatStartState = MutableStateFlow<ChatStartState>(ChatStartState.Idle)
    val chatStartState: StateFlow<ChatStartState> = _chatStartState.asStateFlow()
    
    // Current stream ID
    private var currentStreamId: String? = null
    
    // Current session ID
    private var currentSessionId: String? = null
    
    // Coroutine scope for streaming
    private val streamingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Start a chat with a message
     */
    suspend fun startChat(
        sessionId: String,
        message: String,
        profile: String? = null,
        model: String? = null
    ): Result<ChatStartResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            _chatStartState.value = ChatStartState.Loading
            currentSessionId = sessionId
            
            val request = ChatStartRequest(
                sessionId = sessionId,
                message = message,
                profile = profile,
                model = model
            )
            
            val response = api.startChat(request)
            
            if (response.isSuccessful) {
                val chatResponse = response.body()
                if (chatResponse != null) {
                    currentStreamId = chatResponse.streamId
                    _chatStartState.value = ChatStartState.Success(chatResponse)
                    Result.success(chatResponse)
                } else {
                    _chatStartState.value = ChatStartState.Error("Empty response")
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _chatStartState.value = ChatStartState.Error(error)
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting chat", e)
            _chatStartState.value = ChatStartState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Start listening to chat stream
     */
    fun startStreaming(sessionId: String): Flow<StreamEvent> {
        currentSessionId = sessionId
        
        return sseClient.chatStream(currentStreamId ?: "")
            .filter { it.type != SseEventType.HEARTBEAT }
            .map { event ->
                parseStreamEvent(event)
            }
            .also { flow ->
                streamingScope.launch {
                    flow.collect { event ->
                        handleStreamEvent(event)
                    }
                }
            }
    }
    
    /**
     * Start listening to session-level SSE
     */
    fun startSessionStreaming(sessionId: String): Flow<StreamEvent> {
        return sseClient.sessionStream(sessionId)
            .filter { it.type != SseEventType.HEARTBEAT }
            .map { event ->
                parseStreamEvent(event)
            }
    }
    
    /**
     * Stop current stream
     */
    suspend fun stopStream(): Result<CancelStreamResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val streamId = currentStreamId ?: return@withContext Result.failure(Exception("No active stream"))
            
            val request = CancelStreamRequest(streamId)
            val response = api.cancelStream(request)
            
            if (response.isSuccessful) {
                val cancelResponse = response.body()
                if (cancelResponse != null) {
                    currentStreamId = null
                    _streamingMessageState.value = null
                    _chatState.value = ChatState.IDLE
                    Result.success(cancelResponse)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping stream", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get stream status
     */
    suspend fun getStreamStatus(streamId: String): Result<StreamStatusResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = api.getStreamStatus(streamId)
            
            if (response.isSuccessful) {
                val status = response.body()
                if (status != null) {
                    Result.success(status)
                } else {
                    Result.failure(Exception("Status not found"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(error))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stream status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear current session
     */
    fun clearCurrentSession() {
        currentSessionId = null
        currentStreamId = null
        _messagesState.value = emptyList()
        _streamingMessageState.value = null
        _chatState.value = ChatState.IDLE
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        streamingScope.cancel()
        clearCurrentSession()
    }
    
    private fun parseStreamEvent(event: SseEvent): StreamEvent {
        return try {
            when (event.type) {
                SseEventType.MESSAGE -> {
                    val messageEvent = Gson().fromJson(event.data, StreamMessageEvent::class.java)
                    StreamEvent.MessageEvent(messageEvent)
                }
                SseEventType.TOOL_CALL -> {
                    val toolCallEvent = Gson().fromJson(event.data, ToolCallEvent::class.java)
                    StreamEvent.ToolCallEvent(toolCallEvent)
                }
                SseEventType.TOOL_RESULT -> {
                    val toolResultEvent = Gson().fromJson(event.data, ToolResultEvent::class.java)
                    StreamEvent.ToolResultEvent(toolResultEvent)
                }
                SseEventType.APPROVAL -> {
                    val approvalEvent = Gson().fromJson(event.data, ApprovalEvent::class.java)
                    StreamEvent.ApprovalEvent(approvalEvent)
                }
                SseEventType.DONE -> {
                    val doneEvent = Gson().fromJson(event.data, DoneEvent::class.java)
                    StreamEvent.DoneEvent(doneEvent)
                }
                SseEventType.STREAM_END -> {
                    val streamEndEvent = Gson().fromJson(event.data, StreamEndEvent::class.java)
                    StreamEvent.StreamEndEvent(streamEndEvent)
                }
                SseEventType.ERROR -> {
                    val errorEvent = Gson().fromJson(event.data, ErrorEvent::class.java)
                    StreamEvent.ErrorEvent(errorEvent)
                }
                SseEventType.INITIAL -> {
                    StreamEvent.InitialEvent
                }
                SseEventType.SERVER_TURN_STARTED -> {
                    StreamEvent.ServerTurnStartedEvent
                }
                else -> {
                    StreamEvent.UnknownEvent(event.type, event.data)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stream event", e)
            StreamEvent.ParseErrorEvent(event.type, event.data, e.message)
        }
    }
    
    private fun handleStreamEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.MessageEvent -> {
                val text = event.data.text ?: ""
                if (event.data.done) {
                    // Message complete
                    _streamingMessageState.value = null
                    _chatState.value = ChatState.IDLE
                } else {
                    // Streaming
                    _streamingMessageState.value = text
                    _chatState.value = ChatState.STREAMING
                }
            }
            is StreamEvent.DoneEvent -> {
                _streamingMessageState.value = null
                _chatState.value = ChatState.IDLE
                currentStreamId = null
            }
            is StreamEvent.StreamEndEvent -> {
                _streamingMessageState.value = null
                _chatState.value = ChatState.IDLE
                currentStreamId = null
            }
            is StreamEvent.ErrorEvent -> {
                _streamingMessageState.value = null
                _chatState.value = ChatState.ERROR
                currentStreamId = null
            }
            else -> {
                // Other events don't affect state directly
            }
        }
    }
}

/**
 * Stream events
 */
sealed class StreamEvent {
    data class MessageEvent(val data: StreamMessageEvent) : StreamEvent()
    data class ToolCallEvent(val data: ToolCallEvent) : StreamEvent()
    data class ToolResultEvent(val data: ToolResultEvent) : StreamEvent()
    data class ApprovalEvent(val data: ApprovalEvent) : StreamEvent()
    data class DoneEvent(val data: DoneEvent) : StreamEvent()
    data class StreamEndEvent(val data: StreamEndEvent) : StreamEvent()
    data class ErrorEvent(val data: ErrorEvent) : StreamEvent()
    object InitialEvent : StreamEvent()
    object ServerTurnStartedEvent : StreamEvent()
    data class UnknownEvent(val type: String, val data: String?) : StreamEvent()
    data class ParseErrorEvent(val type: String, val data: String?, val error: String?) : StreamEvent()
}

/**
 * State for chat start
 */
sealed class ChatStartState {
    object Idle : ChatStartState()
    object Loading : ChatStartState()
    data class Success(val response: ChatStartResponse) : ChatStartState()
    data class Error(val message: String) : ChatStartState()
}
