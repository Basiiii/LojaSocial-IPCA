package com.lojasocial.app.ui.applications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.applications.components.ApplicationHeader
import com.lojasocial.app.ui.applications.components.CustomLabelInput
import com.lojasocial.app.ui.applications.components.PhoneInputField
import com.lojasocial.app.ui.theme.LojaSocialPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaStep1View(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    var nome by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var ccPassaporte by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Realizar Candidatura",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onNavigateNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
            ) {
                Text("Seguinte", fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ApplicationHeader(
                title = "Informações Pessoais",
                pageNumber = "Página 1 de 3"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                CustomLabelInput(
                    label = "Nome",
                    value = nome,
                    onValueChange = { nome = it },
                    placeholder = "Insira o nome completo"
                )

                CustomLabelInput(
                    label = "Data de nascimento",
                    value = dataNascimento,
                    onValueChange = { dataNascimento = it },
                    placeholder = "dd/mm/aaaa"
                )

                CustomLabelInput(
                    label = "CC/Passaporte",
                    value = ccPassaporte,
                    onValueChange = { ccPassaporte = it },
                    placeholder = "Introduz número de CC ou Passaporte"
                )

                CustomLabelInput(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Insira aqui o seu email",
                    keyboardType = KeyboardType.Email
                )

                PhoneInputField(
                    value = telemovel,
                    onValueChange = { telemovel = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun CandidaturaStep1Preview() {
    MaterialTheme {
        CandidaturaStep1View()
    }
}