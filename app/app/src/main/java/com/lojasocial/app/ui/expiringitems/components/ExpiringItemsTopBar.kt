package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.TextDark

/**
 * Top app bar component for the expiring items screen.
 * 
 * Provides a consistent navigation header with:
 * - Screen title: "Itens Próximos do Prazo"
 * - Back button: Navigates back to the Employee Portal home screen
 * 
 * The bar uses the app's standard styling with the background color matching
 * the screen background for a seamless look.
 * 
 * @param onNavigateBack Callback invoked when the back button is clicked.
 *                       Should navigate to the Employee Portal home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringItemsTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Itens Próximos do Prazo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextDark
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBgColor)
    )
}

/**
 * Preview composable for the ExpiringItemsTopBar component.
 */
@Preview(showBackground = true)
@Composable
fun ExpiringItemsTopBarPreview() {
    LojaSocialTheme {
        ExpiringItemsTopBar(onNavigateBack = {})
    }
}
