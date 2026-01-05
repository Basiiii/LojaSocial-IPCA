package com.lojasocial.app.ui.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.LojaSocialPrimary

/**
 * Contact section component for the support screen.
 * 
 * This component provides the contact options for users who need
 * additional help beyond the FAQ section.
 * 
 * @param onStartChat Callback when the user clicks to start a chat.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun ContactSection(
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SupportSectionHeader(
            title = "Fale connosco",
            subtitle = "Recebe apoio quando precisares"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ChatStartButton(
            onClick = onStartChat,
            modifier = Modifier.fillMaxWidth()
        )
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
    onClick: () -> Unit,
    text: String = "Abrir Chat",
    modifier: Modifier = Modifier
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
