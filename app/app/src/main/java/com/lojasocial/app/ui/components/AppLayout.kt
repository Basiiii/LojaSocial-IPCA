package com.lojasocial.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lojasocial.app.ui.theme.AppBgColor

@Composable
fun AppLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    subtitle: String,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(subtitle = subtitle)
        },
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        },
        containerColor = AppBgColor
    ) { paddingValues ->
        content(paddingValues)
    }
}
