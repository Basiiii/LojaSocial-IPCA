package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.ScanRed
import com.lojasocial.app.ui.theme.TextDark

/**
 * Error state component for the expiring items screen.
 * 
 * Displays a user-friendly error message when loading expiring items fails.
 * Provides a retry button to allow users to attempt loading the data again.
 * 
 * The component shows:
 * - A warning icon to indicate an error state
 * - The error message explaining what went wrong
 * - A retry button to reload the data
 * 
 * @param errorMessage The error message to display to the user. Should be clear and actionable.
 * @param onRetry Callback invoked when the retry button is clicked. Should trigger a reload of expiring items.
 */
@Composable
fun ExpiringItemsErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = ScanRed
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = TextDark
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LojaSocialPrimary
                )
            ) {
                Text("Tentar Novamente")
            }
        }
    }
}

/**
 * Preview composable for the ExpiringItemsErrorState component.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsErrorStatePreview() {
    LojaSocialTheme {
        ExpiringItemsErrorState(
            errorMessage = "Erro ao carregar itens. Verifique a sua conexão.",
            onRetry = {}
        )
    }
}
