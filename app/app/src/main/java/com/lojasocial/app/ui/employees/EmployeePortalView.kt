package com.lojasocial.app.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.components.StatsSection

@Composable
fun EmployeePortalView(
    useAppLayout: Boolean = true,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null
) {
    val content = @Composable { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            GreetingSection()
            Spacer(modifier = Modifier.height(16.dp))
            StatsSection()
            Spacer(modifier = Modifier.height(24.dp))
            QuickActionsSection()
            Spacer(modifier = Modifier.height(24.dp))
            RecentActivitySection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (useAppLayout) {
        AppLayout(
            selectedTab = "home",
            onTabSelected = {},
            subtitle = "Portal Funcionários",
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun EmployeeScreenPreview() {
    MaterialTheme {
        AppLayout(
            selectedTab = "home",
            onTabSelected = {},
            subtitle = "Portal Funcionários"
        ) { paddingValues ->
            EmployeePortalView(useAppLayout = false)
        }
    }
}