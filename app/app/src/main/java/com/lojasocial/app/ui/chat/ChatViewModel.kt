package com.lojasocial.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.data.api.ChatApiClient
import com.lojasocial.app.data.api.ChatMessageRequest
import com.lojasocial.app.data.api.ChatRequest
import com.lojasocial.app.domain.chat.ChatMessage
import com.lojasocial.app.domain.chat.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing chat state and interactions.
 * 
 * This ViewModel handles all chat-related business logic including:
 * - Managing the chat message list and UI state
 * - Processing user input and sending messages to the API
 * - Handling API responses and error states
 * - Maintaining conversation history
 * 
 * The ViewModel uses a single StateFlow to expose the UI state,
 * making it easy for the UI to observe and react to changes.
 */
class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Initialize chat with a welcome message from the assistant
        addWelcomeMessage()
    }

    /**
     * Updates the input text when the user types in the message field.
     * 
     * @param message The new text content of the input field.
     */
    fun onMessageChange(message: String) {
        _uiState.update { it.copy(inputText = message) }
    }

    /**
     * Sends the current message to the chat API.
     * 
     * This method:
     * 1. Validates the input (ignores empty messages)
     * 2. Adds the user message to the conversation
     * 3. Clears the input field and shows loading state
     * 4. Makes an API call to get the assistant response
     * 5. Handles success and error cases appropriately
     */
    fun sendMessage() {
        val currentMessage = _uiState.value.inputText
        if (currentMessage.isBlank()) return

        // Add user message to conversation
        addMessage(currentMessage, isUser = true)
        
        // Clear input and show loading state
        _uiState.update { 
            it.copy(
                inputText = "", 
                isLoading = true, 
                error = null
            ) 
        }
        
        // Send message to API
        sendToApi(currentMessage)
    }

    /**
     * Sends a message to the chat API and handles the response.
     * 
     * @param message The message to send to the API.
     */
    private fun sendToApi(message: String) {
        viewModelScope.launch {
            try {
                val request = ChatRequest(
                    messages = listOf(
                        ChatMessageRequest(
                            role = "user",
                            content = message
                        )
                    )
                )
                
                val response = ChatApiClient.apiService.sendMessage(
                    authorization = "Bearer lojasocial2025",
                    contentType = "application/json",
                    request = request
                )
                
                handleApiResponse(response)
                
            } catch (e: Exception) {
                handleApiError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Handles successful API response.
     * 
     * @param response The HTTP response from the chat API.
     */
    private fun handleApiResponse(response: retrofit2.Response<com.lojasocial.app.data.api.ChatResponse>) {
        if (response.isSuccessful) {
            val botResponse = response.body()?.choices?.firstOrNull()?.message?.content
                ?: getDefaultErrorResponse()
            addMessage(botResponse, isUser = false)
        } else {
            handleHttpError(response.code())
        }
    }

    /**
     * Handles network or API errors.
     * 
     * @param exception The exception that occurred.
     */
    private fun handleApiError(exception: Exception) {
        val errorMessage = "Desculpe, estou com dificuldades para me conectar. Tente novamente em alguns instantes."
        addMessage(errorMessage, isUser = false)
        _uiState.update { it.copy(error = exception.message) }
    }

    /**
     * Handles HTTP error responses.
     * 
     * @param statusCode The HTTP status code.
     */
    private fun handleHttpError(statusCode: Int) {
        val errorMessage = "Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente."
        addMessage(errorMessage, isUser = false)
        _uiState.update { it.copy(error = "Erro: $statusCode") }
    }

    /**
     * Adds a message to the conversation history.
     * 
     * @param text The message content.
     * @param isUser Whether this is a user message.
     */
    private fun addMessage(text: String, isUser: Boolean) {
        val newMessage = ChatMessage(
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        )
        
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + newMessage
            )
        }
    }

    /**
     * Adds the initial welcome message when the chat is created.
     */
    private fun addWelcomeMessage() {
        val welcomeMessage = "Olá! Sou o assistente virtual da Loja Social. Como posso ajudar você hoje?"
        addMessage(welcomeMessage, isUser = false)
    }

    /**
     * Returns a default error response for when the API doesn't provide one.
     */
    private fun getDefaultErrorResponse(): String {
        return "Desculpe, não consegui entender. Poderia reformular sua pergunta?"
    }
}
