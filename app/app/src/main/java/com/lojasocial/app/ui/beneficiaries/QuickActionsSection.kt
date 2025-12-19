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


@Composable
fun QuickActionsSection() {
    Column {
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
            onClick = { /* Navigate to Order */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Precisas de ajuda?",
            description = "A nossa equipa de suporte está aqui para te ajudar com qualquer pergunta",
            buttonText = "Entra em Contacto",
            backgroundColor = BrandOrange,
            icon = Icons.AutoMirrored.Filled.Help,
            onClick = { /* Navigate to Support */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Pedidos Levantamento",
            description = "Gere os teus pedidos de levantamento de bens",
            buttonText = "Gere Pedidos",
            backgroundColor = com.lojasocial.app.ui.theme.BrandPurple,
            icon = Icons.Default.Inventory2,
            badgeCount = 2,
            badgeLabel = "Pendentes",
            onClick = { /* Navigate to Pickups */ }
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
fun QuickActionsSectionPreview() {
    QuickActionsSection()
}
