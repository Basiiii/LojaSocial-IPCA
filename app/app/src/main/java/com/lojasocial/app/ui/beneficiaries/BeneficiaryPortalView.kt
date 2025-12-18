package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.beneficiaries.ActivitySection
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection

@Composable
fun BeneficiaryPortalView(useAppLayout: Boolean = true) {
    val content = @Composable { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            GreetingSection(
                name = "Carla",
                message = "Recebe apoio quando precisares e acompanha os teus pedidos"
            )

            QuickActionsSection()

            ActivitySection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (useAppLayout) {
        AppLayout(
            selectedTab = "home",
            onTabSelected = {},
            subtitle = "Portal Beneficiários"
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
