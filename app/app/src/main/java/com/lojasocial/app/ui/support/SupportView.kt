package com.lojasocial.app.ui.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.support.components.ContactSection
import com.lojasocial.app.ui.support.components.FaqSection
import com.lojasocial.app.ui.support.components.SupportTopBar
import com.lojasocial.app.domain.support.FaqRepository

/**
 * Main support screen view.
 * 
 * This screen provides users with access to frequently asked questions
 * and contact options for additional support. It features a clean layout
 * with expandable FAQ items and a prominent chat initiation button.
 * 
 * @param onStartChat Callback when the user wants to start a chat session.
 * @param onNavigateBack Optional callback for back navigation.
 * @param showTopBar Whether to show the top app bar.
 * @param showBackButton Whether to show the back button in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportView(
    onStartChat: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    showBackButton: Boolean = true
) {
    val content: @Composable (PaddingValues) -> Unit = { padding ->
        SupportContent(
            paddingValues = padding,
            onStartChat = onStartChat
        )
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                SupportTopBar(
                    showBackButton = showBackButton,
                    onNavigateBack = onNavigateBack
                )
            },
            containerColor = Color.White
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(androidx.compose.foundation.layout.PaddingValues(0.dp))
    }
}

/**
 * Main content layout for the support screen.
 * 
 * This component contains the FAQ section and contact section
 * with proper scrolling and spacing.
 * 
 * @param paddingValues Padding values from the scaffold.
 * @param onStartChat Callback when the user wants to start a chat.
 */
@Composable
private fun SupportContent(
    paddingValues: PaddingValues,
    onStartChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Contact Section (moved to top)
        ContactSection(
            onStartChat = onStartChat,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // FAQ Section (moved below chat button)
        FaqSection(
            faqList = FaqRepository.getFaqItems(),
            modifier = Modifier.padding(bottom = 40.dp)
        )
    }
}

/**
 * Preview composable for the SupportView.
 * 
 * This preview shows the support screen with sample data
 * for design and development purposes.
 */
@Preview(showBackground = true)
@Composable
fun SupportViewPreview() {
    SupportView(
        onStartChat = {}
    )
}
