package com.lojasocial.app.domain.chat

import java.util.*

/**
 * Domain model representing a single chat message.
 * 
 * This entity represents a chat message in the domain layer, containing
 * all essential information about a message independent of UI concerns.
 * 
 * @property id Unique identifier for the message.
 * @property text The message content.
 * @property isUser Whether the message was sent by the user (true) or assistant (false).
 * @property timestamp The timestamp when the message was created.
 * @property isLoading Whether this message is currently in a loading state (showing loading indicator).
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

/**
 * Domain model representing the UI state of the chat interface.
 * 
 * This state class contains all the observable state that the chat UI needs
 * to render properly, including messages, input state, and loading states.
 * While this is UI-specific state, it's defined in the domain layer to
 * maintain clean separation between UI logic and state management.
 * 
 * @property messages List of all chat messages in chronological order.
 * @property inputText Current text in the input field.
 * @property isLoading Whether the chat is currently processing a message.
 * @property error Optional error message if something went wrong.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
