package com.lojasocial.app.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandOrange
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark

@Composable
fun QuickActionsSection(
    onNavigateToScanStock: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    onNavigateToPickupRequests: () -> Unit = {},
    onNavigateToUrgentRequest: () -> Unit = {},
    pendingRequestsCount: Int? = null,
    pendingApplicationsCount: Int = 0
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
            badgeCount = if (pendingRequestsCount != null && pendingRequestsCount > 0) pendingRequestsCount else null,
            badgeLabel = if (pendingRequestsCount != null && pendingRequestsCount > 0) "Pendentes" else null,
            onClick = onNavigateToPickupRequests
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Precisas de ajuda?",
            description = "A nossa equipa de suporte está aqui para te ajudar com qualquer pergunta",
            buttonText = "Entra em Contacto",
            backgroundColor = BrandOrange,
            icon = Icons.AutoMirrored.Filled.Help,
            onClick = onSupportClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Ver Candidaturas",
            description = "Revê e processa candidaturas pendentes",
            buttonText = "Rever Candidaturas",
            backgroundColor = BrandBlue,
            icon = Icons.Default.Description,
            badgeCount = if (pendingApplicationsCount > 0) pendingApplicationsCount else null,
            badgeLabel = if (pendingApplicationsCount > 0) "Novas" else null,
            isRedBadge = true,
            onClick = onNavigateToApplications
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Entregas Urgentes",
            description = "Cria uma entrega urgente e concluída imediatamente",
            buttonText = "Criar Entrega",
            backgroundColor = Color(0xFFDC2626),
            icon = Icons.Default.LocalShipping,
            onClick = onNavigateToUrgentRequest
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun QuickActionsSectionPreview() {
    QuickActionsSection(
        onSupportClick = {}
    )
}
