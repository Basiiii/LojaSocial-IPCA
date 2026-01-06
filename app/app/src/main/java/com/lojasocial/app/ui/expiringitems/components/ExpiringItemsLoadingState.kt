package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.LojaSocialTheme

/**
 * Loading state component for the expiring items screen.
 * 
 * Displays a centered circular progress indicator while expiring items
 * are being fetched from the repository. This provides visual feedback
 * to the user that data is being loaded.
 * 
 * The indicator uses the app's primary color to maintain visual consistency.
 */
@Composable
fun ExpiringItemsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LojaSocialPrimary)
    }
}

/**
 * Preview composable for the ExpiringItemsLoadingState component.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpiringItemsLoadingStatePreview() {
    LojaSocialTheme {
        ExpiringItemsLoadingState()
    }
}
