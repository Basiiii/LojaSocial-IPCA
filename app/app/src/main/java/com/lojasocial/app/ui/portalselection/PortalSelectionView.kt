package com.lojasocial.app.ui.portalselection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.components.AppTopBar
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun PortalSelectionView(
    onNavigateToEmployeePortal: () -> Unit,
    onNavigateToBeneficiaryPortal: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(subtitle = "Área Pessoal") },
        containerColor = AppBgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {

            // Título de Boas-vindas
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Olá, Manel",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Left
            )
            Text(
                text = "O que pretendes fazer hoje?",
                fontSize = 16.sp,
                color = TextGray,
                textAlign = TextAlign.Left
            )
            Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Seleciona o Portal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Botão Portal Funcionário
                ActionCard(
                    title = "Portal Funcionário",
                    description = "Gestão de stocks, pedidos e candidaturas.",
                    buttonText = "Entrar como Staff",
                    backgroundColor = BrandBlue,
                    icon = null,
                    onClick = onNavigateToEmployeePortal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Portal Beneficiário
                ActionCard(
                    title = "Portal Beneficiário",
                    description = "Acede aos teus pedidos e apoios.",
                    buttonText = "Aceder aos meus Pedidos",
                    backgroundColor = BrandPurple,
                    icon = null,
                    onClick = onNavigateToBeneficiaryPortal
                )


            Spacer(modifier = Modifier.weight(1f))

            // Botão de Logout
            Spacer(modifier = Modifier.height(32.dp))
            Column (modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center){
                TextButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terminar Sessão", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }

        }
    }
}

@Preview(name = "1. Staff e Beneficiário", showBackground = true, heightDp = 800)
@Composable
fun PreviewBothRoles() {
    PortalSelectionView(onNavigateToEmployeePortal = {}, onNavigateToBeneficiaryPortal = {}, onLogout = {})
}
