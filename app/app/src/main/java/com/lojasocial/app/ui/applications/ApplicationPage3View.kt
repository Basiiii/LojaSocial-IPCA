package com.lojasocial.app.ui.applications

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.provider.OpenableColumns
import com.lojasocial.app.ui.applications.components.ApplicationHeader
import com.lojasocial.app.ui.applications.components.DocumentoUi
import com.lojasocial.app.ui.applications.components.DocumentUploadCard
import com.lojasocial.app.ui.applications.components.AddDocumentButton
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaStep3View(
    onNavigateBack: () -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    val context = LocalContext.current
    var documentos by remember {
        mutableStateOf(
            listOf(
                DocumentoUi(id = 1, nome = "Comprovativo de Inscrição no IPCA"),
                DocumentoUi(id = 2, nome = "Comprovativo de Rendimento"),
                DocumentoUi(id = 3, nome = "Declaração IRS"),
                DocumentoUi(id = 4, nome = "Outro Documento")
            )
        )
    }

    var nextId by remember { mutableStateOf(5) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileName = getFileName(context, selectedUri)
            val novoId = nextId
            nextId++
            documentos = documentos + DocumentoUi(novoId, fileName, selectedUri)
        }
    }

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
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGray, contentColor = Color.Black)
                ) {
                    Text("Anterior", fontSize = 16.sp)
                }

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary)
                ) {
                    Text("Submeter", fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ApplicationHeader(
                title = "Documentos a entregar",
                pageNumber = "Página 3 de 3"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Anexar Ficheiros",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = com.lojasocial.app.ui.applications.components.TextGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            AddDocumentButton(onAddDocument = {
                filePickerLauncher.launch("application/pdf")
            })

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                documentos.forEach { doc ->
                    DocumentUploadCard(
                        document = doc,
                        onDelete = {
                            documentos = documentos.filter { it.id != doc.id }
                        },
                        onUpload = { uri ->
                            val updatedDocumentos = documentos.map {
                                if (it.id == doc.id) it.copy(uri = uri)
                                else it
                            }
                            documentos = updatedDocumentos
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Ficheiro sem nome"
}

@Preview(showBackground = true)
@Composable
fun CandidaturaStep3Preview() {
    MaterialTheme {
        CandidaturaStep3View()
    }
}