package com.lojasocial.app.ui.portalselection
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.components.AppTopBar
import com.lojasocial.app.ui.theme.*

@Composable
fun PortalSelectionView(
    userName: String,
    onNavigateToEmployeePortal: () -> Unit,
    onNavigateToBeneficiaryPortal: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(subtitle = "Escolher Portal", notifications = false) },
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
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Olá, $userName",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                textAlign = TextAlign.Left
            )
            Text(
                text = "O que pretendes fazer hoje?",
                fontSize = 16.sp,
                color = TextGray,
                textAlign = TextAlign.Left
            )
            Spacer(modifier = Modifier.height(22.dp))
                // Portal Funcionário
                ActionCard(
                    title = "Portal Funcionário",
                    description = "Acompanha candidaturas, controla pedidos e gere o stock facilmente",
                    buttonText = "Entrar",
                    backgroundColor = BrandBlue,
                    icon = null,
                    onClick = onNavigateToEmployeePortal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Portal Beneficiário
                ActionCard(
                    title = "Portal Beneficiário",
                    description = "Recebe apoio quando precisares e acompanha os teus pedidos",
                    buttonText = "Entrar",
                    backgroundColor = BrandPurple,
                    icon = null,
                    onClick = onNavigateToBeneficiaryPortal
                )

            Spacer(modifier = Modifier.weight(1f))

//            Column (modifier = Modifier
//                .padding(paddingValues)
//                .fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center){
//                TextButton(
//                    onClick = onLogout,
//                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
//                    modifier = Modifier.padding(bottom = 16.dp),
//                ) {
//                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Terminar Sessão", fontWeight = FontWeight.Medium, fontSize = 16.sp)
//                }
//            }
        }
    }
}
