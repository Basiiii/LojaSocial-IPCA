package com.lojasocial.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lojasocial.app.ui.chat.components.ChatHeader
import com.lojasocial.app.ui.chat.components.ChatInputBar
import com.lojasocial.app.ui.chat.components.ChatMessageBubble

/**
 * Main chat interface component.
 * 
 * This component provides a complete chat interface with message display,
 * input functionality, and proper state management. It supports both
 * embedded and standalone layouts with optional close functionality.
 * 
 * @param embeddedInAppLayout Whether the chat is embedded in the app layout.
 * @param onClose Optional callback when the close button is pressed.
 * @param viewModel The chat view model for state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    embeddedInAppLayout: Boolean = true,
    onClose: (() -> Unit)? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to latest message when messages change
    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatHeader(
                onClose = onClose,
                showCloseButton = onClose != null
            )
        },
        bottomBar = {
            ChatInputBar(
                value = uiState.inputText,
                onValueChange = viewModel::onMessageChange,
                onSendClick = {
                    viewModel.sendMessage()
                    keyboardController?.hide()
                },
                modifier = if (embeddedInAppLayout) {
                    Modifier.imePadding()
                } else {
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        ChatMessageList(
            messages = uiState.messages,
            isLoading = uiState.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Message list component for displaying chat messages.
 * 
 * This component handles the display of all chat messages with proper
 * scrolling and loading state management.
 * 
 * @param messages The list of messages to display.
 * @param isLoading Whether the chat is currently loading.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Preview composable for the ChatView in embedded layout.
 * 
 * This preview shows the chat interface as it would appear
 * when embedded within the app layout with a close button.
 */
@Preview(showBackground = true, name = "Chat View - Embedded")
@Composable
fun ChatViewEmbeddedPreview() {
    ChatView(
        embeddedInAppLayout = true,
        onClose = { }
    )
}

/**
 * Preview composable for the ChatView in standalone layout.
 * 
 * This preview shows the chat interface as it would appear
 * as a standalone screen without a close button.
 */
@Preview(showBackground = true, name = "Chat View - Standalone")
@Composable
fun ChatViewStandalonePreview() {
    ChatView(
        embeddedInAppLayout = false,
        onClose = null
    )
}
