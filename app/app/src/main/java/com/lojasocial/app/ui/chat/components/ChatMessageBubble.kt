package com.lojasocial.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.chat.ChatMessage
import com.lojasocial.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * A message bubble component for displaying chat messages.
 * 
 * This component renders a single chat message with appropriate styling
 * based on whether the message is from the user or the assistant.
 * It includes the message text, timestamp, and proper alignment.
 * 
 * @param message The chat message to display.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser

    // Bubble Shape: User messages round on left, Assistant messages round on right
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val backgroundColor = if (isUser) ChatBrandGreen else ChatBubbleGray
    val contentColor = if (isUser) Color.White else ChatTextDark
    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp) // Max width of bubble
                .clip(bubbleShape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = contentColor,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // Tiny timestamp below bubble
        Text(
            text = formatTime(message.timestamp),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

/**
 * Utility function for formatting message timestamps.
 * 
 * @param timestamp The timestamp in milliseconds.
 * @return Formatted time string in HH:mm format.
 */
fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Preview composable for user message bubble.
 * 
 * This preview shows a message bubble as it would appear
 * when sent by the user.
 */
@Preview(showBackground = true, name = "User Message")
@Composable
fun UserMessageBubblePreview() {
    val message = ChatMessage(
        text = "Olá! Preciso de ajuda com o meu pedido.",
        isUser = true,
        timestamp = System.currentTimeMillis()
    )
    
    ChatMessageBubble(message = message)
}

/**
 * Preview composable for assistant message bubble.
 * 
 * This preview shows a message bubble as it would appear
 * when sent by the AI assistant.
 */
@Preview(showBackground = true, name = "Assistant Message")
@Composable
fun AssistantMessageBubblePreview() {
    val message = ChatMessage(
        text = "Olá! Sou o assistente virtual da Loja Social. Como posso ajudar você hoje?",
        isUser = false,
        timestamp = System.currentTimeMillis()
    )
    
    ChatMessageBubble(message = message)
}

/**
 * Preview composable for long message bubble.
 * 
 * This preview shows how a longer message is handled
 * with proper text wrapping and bubble sizing.
 */
@Preview(showBackground = true, name = "Long Message")
@Composable
fun LongMessageBubblePreview() {
    val message = ChatMessage(
        text = "Este é um exemplo de uma mensagem mais longa que demonstra como o componente lida com texto extenso, fazendo o wrapping adequado e mantendo o tamanho máximo da bolha de 280dp conforme definido no design.",
        isUser = false,
        timestamp = System.currentTimeMillis()
    )
    
    ChatMessageBubble(message = message)
}
