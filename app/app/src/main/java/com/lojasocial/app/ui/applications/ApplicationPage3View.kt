package com.lojasocial.app.ui.applications

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import android.net.Uri
import android.provider.OpenableColumns
import com.lojasocial.app.domain.ApplicationDocument
import com.lojasocial.app.repository.ApplicationRepositoryImpl
import com.lojasocial.app.ui.applications.components.ApplicationHeader
import com.lojasocial.app.ui.applications.components.DocumentUi
import com.lojasocial.app.ui.applications.components.DocumentUploadCard
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaStep3View(
    onNavigateBack: () -> Unit = {},
    onSubmit: () -> Unit = {},
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val formData by viewModel.formData.collectAsState()

    // Set context in repository for file operations
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    // Fixed list of required documents
    var documents by remember {
        mutableStateOf(
            listOf(
                DocumentUi(id = 1, name = "Comprovativo de Inscrição no IPCA"),
                DocumentUi(id = 2, name = "Documento de Apoio FAES"),
                DocumentUi(id = 3, name = "Documento de Bolsa")
            )
        )
    }

    // Sync documents with ViewModel
    LaunchedEffect(documents) {
        val applicationDocuments = documents.map { doc ->
            ApplicationDocument(
                id = doc.id,
                name = doc.name,
                uri = doc.uri
            )
        }
        viewModel.documents = applicationDocuments
    }

    // Handle submission success/error
    LaunchedEffect(uiState.submissionSuccess) {
        if (uiState.submissionSuccess) {
            viewModel.clearSubmissionState()
            onSubmit()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Submeter Candidatura",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
            ) {
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
                        onClick = {
                            viewModel.submitApplication()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LojaSocialPrimary),
                        enabled = !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Submeter", fontSize = 16.sp)
                        }
                    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = com.lojasocial.app.ui.applications.components.TextGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                documents.forEach { doc ->
                    DocumentUploadCard(
                        document = doc,
                        onDelete = {
                            val updatedDocuments = documents.map {
                                if (it.id == doc.id) it.copy(uri = null)
                                else it
                            }
                            documents = updatedDocuments
                        },
                        onUpload = { uri ->
                            val updatedDocuments = documents.map {
                                if (it.id == doc.id) it.copy(uri = uri)
                                else it
                            }
                            documents = updatedDocuments
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            uiState.submissionError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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
fun CandidaturaStep3Preview(
) {
    MaterialTheme {
        CandidaturaStep3View(onNavigateBack = {}, onSubmit = {}, viewModel = hiltViewModel())
    }
}