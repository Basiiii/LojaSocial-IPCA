package com.lojasocial.app.ui.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.LojaSocialPrimary

/**
 * Contact section component for the support screen.
 * 
 * This component displays a prominent card with chat support information
 * and provides the main entry point for users to start a chat session.
 * It features a clean design with a title, subtitle, and chat icon.
 * 
 * @param onStartChat Callback when the user wants to start a chat session.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun ContactSection(
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onStartChat,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = LojaSocialPrimary,
            contentColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Precisa de ajuda?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fale com a nossa equipa em tempo real",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // A clean icon circle
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Primary call-to-action button for starting a chat session.
 * 
 * This button provides the main entry point for users to access
 * the chat support functionality.
 * 
 * @param onClick Callback when the button is pressed.
 * @param text The text to display on the button.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun ChatStartButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String = "Abrir Chat"
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = LojaSocialPrimary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
