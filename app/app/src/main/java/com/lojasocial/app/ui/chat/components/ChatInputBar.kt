package com.lojasocial.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.*

/**
 * Input bar component for composing and sending chat messages.
 * 
 * This component provides a text input field with a send button for
 * users to compose and send messages in the chat interface.
 * 
 * @param value The current text input value.
 * @param onValueChange Callback when the input text changes.
 * @param onSendClick Callback when the send button is clicked.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text Input
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Digite sua mensagem...", fontSize = 14.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ChatBubbleGray,
                unfocusedContainerColor = ChatBubbleGray,
                disabledContainerColor = ChatBubbleGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = onSendClick,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(ChatBrandGreen)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = Color.White,
                modifier = Modifier.padding(start = 2.dp) // Visual center adjustment
            )
        }
    }
}

/**
 * Preview composable for the ChatInputBar component.
 * 
 * This preview shows the input bar with sample text for design
 * and development purposes.
 */
@Preview(showBackground = true, name = "Chat Input Bar")
@Composable
fun ChatInputBarPreview() {
    var text by remember { mutableStateOf("Olá, como posso ajudar?") }
    
    ChatInputBar(
        value = text,
        onValueChange = { text = it },
        onSendClick = { }
    )
}

/**
 * Preview composable for the ChatInputBar component with empty text.
 * 
 * This preview shows the input bar in its default empty state.
 */
@Preview(showBackground = true, name = "Chat Input Bar - Empty")
@Composable
fun ChatInputBarEmptyPreview() {
    var text by remember { mutableStateOf("") }
    
    ChatInputBar(
        value = text,
        onValueChange = { text = it },
        onSendClick = { }
    )
}
