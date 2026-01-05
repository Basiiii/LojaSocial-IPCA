package com.lojasocial.app.ui.applications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.applications.components.ApplicationHeader
import com.lojasocial.app.ui.applications.components.CustomLabelInput
import com.lojasocial.app.ui.applications.components.DropdownInputField
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaStep2View(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    var grauAcademico by remember { mutableStateOf("") }
    var curso by remember { mutableStateOf("") }
    var numeroEstudante by remember { mutableStateOf("") }
    var apoioFaes by remember { mutableStateOf("") }
    var temBolsa by remember { mutableStateOf("") }

    val grausList = listOf("Licenciatura", "Mestrado", "CTeSP", "Pós-Graduação")
    val cursosList = listOf("Engenharia Informática", "Design Gráfico", "Gestão", "Solicitadoria")
    val simNaoList = listOf("Sim", "Não")

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Anterior", fontSize = 16.sp)
                }

                Button(
                    onClick = onNavigateNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
                ) {
                    Text("Seguinte", fontSize = 16.sp)
                }
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
                title = "Dados académicos",
                pageNumber = "Página 2 de 3"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                DropdownInputField(
                    label = "Grau Académico",
                    value = grauAcademico,
                    options = grausList,
                    onValueChange = { grauAcademico = it }
                )

                DropdownInputField(
                    label = "Curso",
                    value = curso,
                    options = cursosList,
                    onValueChange = { curso = it }
                )

                CustomLabelInput(
                    label = "Número de Estudante",
                    value = numeroEstudante,
                    onValueChange = { numeroEstudante = it },
                    placeholder = "Introduz número de estudante"
                )

                DropdownInputField(
                    label = "É apoiado(a) pelo Fundo de Apoio de Emergência Social (FAES)?",
                    value = apoioFaes,
                    options = simNaoList,
                    onValueChange = { apoioFaes = it }
                )

                DropdownInputField(
                    label = "É beneficiário de alguma bolsa de estudo ou apoio no presente ano letivo?",
                    value = temBolsa,
                    options = simNaoList,
                    onValueChange = { temBolsa = it }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun CandidaturaStep2Preview() {
    MaterialTheme {
        CandidaturaStep2View()
    }
}