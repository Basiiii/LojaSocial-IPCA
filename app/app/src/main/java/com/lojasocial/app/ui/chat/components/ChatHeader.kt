package com.lojasocial.app.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.ChatBrandGreen

/**
 * Header component for the chat interface.
 * 
 * This component displays the chat title, status indicator, and
 * optionally a close button. It provides a consistent header
 * for the chat interface across different contexts.
 * 
 * @param onClose Optional callback when the close button is pressed.
 * @param showCloseButton Whether to show the close button.
 * @param modifier Optional modifier for styling and layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    showCloseButton: Boolean = onClose != null
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Assistente Virtual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Online agora",
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    color = ChatBrandGreen
                )
            }
        },
        actions = {
            if (showCloseButton && onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        ),
        modifier = modifier
    )
}

/**
 * Preview composable for the ChatHeader component with close button.
 * 
 * This preview shows the header with the close button visible,
 * as it would appear in an embedded chat layout.
 */
@Preview(showBackground = true, name = "Chat Header - With Close")
@Composable
fun ChatHeaderWithClosePreview() {
    ChatHeader(
        onClose = { },
        showCloseButton = true
    )
}

/**
 * Preview composable for the ChatHeader component without close button.
 * 
 * This preview shows the header without the close button,
 * as it would appear in a standalone chat screen.
 */
@Preview(showBackground = true, name = "Chat Header - No Close")
@Composable
fun ChatHeaderNoClosePreview() {
    ChatHeader(
        onClose = null,
        showCloseButton = false
    )
}
