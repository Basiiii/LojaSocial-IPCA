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
    showTopBar: Boolean = true,
    showBottomBar: Boolean = true,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    onActivityClick: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    subtitle = subtitle,
                    showPortalSelection = showPortalSelection,
                    onPortalSelectionClick = onPortalSelectionClick,
                    onActivityClick = onActivityClick
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        },
        containerColor = AppBgColor
    ) { paddingValues ->
        content(paddingValues)
    }
}
