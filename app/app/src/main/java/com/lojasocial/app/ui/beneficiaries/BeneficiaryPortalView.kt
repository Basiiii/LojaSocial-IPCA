package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection

@Composable
fun BeneficiaryPortalView(
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

            GreetingSection(
                name = "Carla",
                message = "Recebe apoio quando precisares e acompanha os teus pedidos"
            )

            Spacer(modifier = Modifier.height(16.dp))

            QuickActionsSection()

            Spacer(modifier = Modifier.height(24.dp))

            ActivitySection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (useAppLayout) {
        AppLayout(
            selectedTab = "home",
            onTabSelected = {},
            subtitle = "Portal Beneficiários",
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun BeneficiaryPreview() {
    MaterialTheme {
        BeneficiaryPortalView()
    }
}