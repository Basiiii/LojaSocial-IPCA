package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

/**
 * Empty state component for the expiring items screen.
 * 
 * Displays a positive message when there are no items expiring within the
 * threshold period (3 days). This indicates that all stock items have
 * expiration dates that are more than 3 days away, which is a good state.
 * 
 * The component shows:
 * - A warning icon (in gray to indicate informational state, not urgent)
 * - A title message: "Nenhum item próximo do prazo"
 * - A subtitle explaining that all items are within their validity period
 * 
 * This provides reassurance to administrators that no immediate action is needed.
 */
@Composable
fun ExpiringItemsEmptyState() {
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
                contentDescription = "No items",
                modifier = Modifier.size(64.dp),
                tint = TextGray
            )
            Text(
                text = "Nenhum item próximo do prazo",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark
            )
            Text(
                text = "Todos os itens estão dentro do prazo de validade",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
    }
}

/**
 * Preview composable for the ExpiringItemsEmptyState component.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsEmptyStatePreview() {
    LojaSocialTheme {
        ExpiringItemsEmptyState()
    }
}
