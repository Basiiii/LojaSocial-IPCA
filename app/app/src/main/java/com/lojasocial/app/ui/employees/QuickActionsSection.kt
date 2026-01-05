package com.lojasocial.app.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.stock.AddStockScreen
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark

@Composable
fun QuickActionsSection(
    onNavigateToScanStock: () -> Unit = {}
) {
    Column {
        Text(
            text = "Ações Rápidas",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Adiciona ao Stock",
            description = "Adiciona um artigo ao stock, fazendo scan do código barras",
            buttonText = "Adiciona Item",
            backgroundColor = BrandGreen,
            icon = Icons.Default.QrCodeScanner,
            onClick = onNavigateToScanStock
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Pedidos Levantamento",
            description = "Gere pedidos de levantamento de bens",
            buttonText = "Gere Pedidos",
            backgroundColor = BrandPurple,
            icon = Icons.Default.Inventory2,
            badgeCount = 12,
            badgeLabel = "Pendentes",
            onClick = { /* Handle Orders */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Ver Candidaturas",
            description = "Revê e processa candidaturas pendentes",
            buttonText = "Rever Candidaturas",
            backgroundColor = BrandBlue,
            icon = Icons.Default.Description,
            badgeCount = 5,
            badgeLabel = "Novas",
            isRedBadge = true,
            onClick = { /* Handle Applications */ }
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
fun QuickActionsSectionPreview() {
    QuickActionsSection()
}
