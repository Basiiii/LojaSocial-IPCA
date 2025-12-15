package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.AppTopBar
import com.lojasocial.app.ui.components.BottomNavigationBar
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.employees.ActivityItem

import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark

val BrandOrange = Color(0xFFD97706)

@Composable
fun BeneficiaryPortalView(useAppLayout: Boolean = true) {
    if (useAppLayout) {
        AppLayout(
            selectedTab = "home",
            onTabSelected = {},
            subtitle = "Portal Beneficiários"
        ) { paddingValues ->
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

            Text(
                text = "Ações Rápidas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(12.dp))


            ActionCard(
                title = "Faz Pedido",
                description = "Vê o que temos e faz um pedido de bens que precisas",
                buttonText = "Faz Pedido",
                backgroundColor = BrandGreen,
                icon = null,
                onClick = { /* Navigate to Order */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionCard(
                title = "Precisas de ajuda?",
                description = "A nossa equipa de suporte está aqui para te ajudar com qualquer pergunta",
                buttonText = "Entra em Contacto",
                backgroundColor = BrandOrange,
                icon = null,
                onClick = { /* Navigate to Support */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionCard(
                title = "Pedidos Levantamento",
                description = "Gere os teus pedidos de levantamento de bens",
                buttonText = "Gere Pedidos",
                backgroundColor = BrandPurple,
                icon = Icons.Default.Inventory2,
                badgeCount = 2,
                badgeLabel = "Pendentes",
                onClick = { /* Navigate to Pickups */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            BeneficiaryActivitySection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    } else {
        Column(
            modifier = Modifier
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

            Text(
                text = "Ações Rápidas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(12.dp))


            ActionCard(
                title = "Faz Pedido",
                description = "Vê o que temos e faz um pedido de bens que precisas",
                buttonText = "Faz Pedido",
                backgroundColor = BrandGreen,
                icon = null,
                onClick = { /* Navigate to Order */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionCard(
                title = "Precisas de ajuda?",
                description = "A nossa equipa de suporte está aqui para te ajudar com qualquer pergunta",
                buttonText = "Entra em Contacto",
                backgroundColor = BrandOrange,
                icon = null,
                onClick = { /* Navigate to Support */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionCard(
                title = "Pedidos Levantamento",
                description = "Gere os teus pedidos de levantamento de bens",
                buttonText = "Gere Pedidos",
                backgroundColor = BrandPurple,
                icon = Icons.Default.Inventory2,
                badgeCount = 2,
                badgeLabel = "Pendentes",
                onClick = { /* Navigate to Pickups */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            BeneficiaryActivitySection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BeneficiaryActivitySection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Atividade Recente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            title = "Levantamento Concluído",
            subtitle = "Levantamento feito com sucesso",
            time = "há 2 minutos",
            icon = Icons.Default.ShoppingBag,
            iconBg = Color(0xFFDBEAFE),
            iconTint = BrandBlue
        )

        ActivityItem(
            title = "Pedido Aceite",
            subtitle = "O teu pedido foi aceite",
            time = "há 15 minutos",
            icon = Icons.Default.Check,
            iconBg = Color(0xFFDCFCE7),
            iconTint = BrandGreen
        )

        ActivityItem(
            title = "Pedido Submetido",
            subtitle = "Pedido submetido e pendente",
            time = "há 1 hora",
            icon = Icons.Default.Schedule,
            iconBg = Color(0xFFFEF3C7),
            iconTint = BrandOrange
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun BeneficiaryPreview() {
    MaterialTheme {
        BeneficiaryPortalView()
    }
}