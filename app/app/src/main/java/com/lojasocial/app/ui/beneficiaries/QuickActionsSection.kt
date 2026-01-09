package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.theme.BrandOrange

/**
 * Quick actions section component for the beneficiary portal.
 * 
 * This component displays a collection of action cards that provide
 * quick access to common beneficiary functions including making orders,
 * accessing support, and managing pickup requests. It serves as the main
 * navigation hub for beneficiary activities.
 * 
 * The section includes:
 * - Order creation functionality
 * - Support contact access
 * - Pickup request management with badge notifications
 * 
 * @param onSupportClick Callback when the user wants to access support features.
 * @param modifier Optional modifier for styling and layout.
 */
@Composable
fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onNavigateToOrders: () -> Unit,
    onNavigateToPickups: () -> Unit,
    onSupportClick: () -> Unit = {},
    pendingRequestsCount: Int? = null
) {
    Column(modifier = modifier) {
        Text(
            text = "Ações Rápidas",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = com.lojasocial.app.ui.theme.TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Faz Pedido",
            description = "Vê o que temos e faz um pedido de bens que precisas",
            buttonText = "Faz Pedido",
            backgroundColor = com.lojasocial.app.ui.theme.BrandGreen,
            icon = Icons.Default.ShoppingCart,
            onClick = onNavigateToOrders
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
            title = "Pedidos Levantamento",
            description = "Gere os teus pedidos de levantamento de bens",
            buttonText = "Gere Pedidos",
            backgroundColor = com.lojasocial.app.ui.theme.BrandPurple,
            icon = Icons.Default.Inventory2,
            badgeCount = if (pendingRequestsCount != null && pendingRequestsCount > 0) pendingRequestsCount else null,
            badgeLabel = if (pendingRequestsCount != null && pendingRequestsCount > 0) "Pendentes" else null,
            onClick = onNavigateToPickups
        )
    }
}

/**
 * Preview composable for the QuickActionsSection component.
 * 
 * This preview shows the quick actions section with all action cards
 * for design and development purposes. It demonstrates the layout
 * and styling of the beneficiary navigation options.
 */
@Preview(showBackground = true, heightDp = 600)
@Composable
fun QuickActionsSectionPreview() {
    QuickActionsSection(
        onNavigateToOrders = {},
        onNavigateToPickups = {},
        onSupportClick = {}
    )
}
