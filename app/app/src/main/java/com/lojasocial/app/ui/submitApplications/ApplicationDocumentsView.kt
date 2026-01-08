package com.lojasocial.app.ui.submitApplications

import android.content.Context
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
import android.widget.Toast
import com.lojasocial.app.domain.application.ApplicationDocument
import com.lojasocial.app.ui.submitApplications.components.ApplicationHeader
import com.lojasocial.app.ui.submitApplications.components.DocumentUi
import com.lojasocial.app.ui.submitApplications.components.DocumentUploadCard
import com.lojasocial.app.ui.theme.ButtonGray
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel

/**
 * Third and final page of the scholarship application form - Document Upload.
 *
 * This composable displays the document upload section of the scholarship
 * application form. It handles the collection and management of required
 * supporting documents for the application submission.
 * 
 * Features:
 * - Manages required document uploads (enrollment proof, FAES support, scholarship documents)
 * - Provides file selection through system file picker
 * - Displays upload status and file names
 * - Handles document deletion and replacement
 * - Manages form submission with loading states
 * - Shows submission errors and success feedback
 * - Maintains form state using ViewModel with StateFlow
 * - Uses Portuguese labels and messages for user interface
 * 
 * @param onNavigateBack Callback for navigating to previous form page
 * @param onSubmit Callback invoked when application is successfully submitted
 * @param viewModel ViewModel for managing form state and business logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidaturaDocumentsView(
    onNavigateBack: () -> Unit = {},
    onSubmit: () -> Unit = {},
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    /**
     * Android context for file operations.
     */
    val context = LocalContext.current
    
    /**
     * Current UI state including loading, submission status, and errors.
     */
    val uiState by viewModel.uiState.collectAsState()
    
    /**
     * Current form data from the ViewModel.
     */
    val formData by viewModel.formData.collectAsState()

    /**
     * Sets the Android context in the repository for file operations.
     * This is required for the repository to access the ContentResolver
     * for reading file URIs and converting them to Base64 format.
     */
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    /**
     * Fixed list of required documents for the scholarship application.
     * 
     * These documents are mandatory for all scholarship applications:
     * 1. Proof of IPCA enrollment
     * 2. FAES support documentation
     * 3. Scholarship documentation
     */
    var documents by remember {
        mutableStateOf(
            listOf(
                DocumentUi(id = 1, name = "Comprovativo de Inscrição no IPCA"),
                DocumentUi(id = 2, name = "Documento de Apoio FAES"),
                DocumentUi(id = 3, name = "Documento de Bolsa")
            )
        )
    }

    /**
     * Synchronizes the local documents state with the ViewModel.
     * 
     * This ensures that any changes to document URIs are reflected
     * in the ViewModel's form data for submission.
     */
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

    /**
     * Handles successful application submission.
     * 
     * When the submission is successful, this effect shows a success toast,
     * clears the submission state and invokes the onSubmit callback to navigate away from the form.
     */
    LaunchedEffect(uiState.submissionSuccess) {
        if (uiState.submissionSuccess) {
            Toast.makeText(
                context,
                "Candidatura submetida com sucesso!",
                Toast.LENGTH_LONG
            ).show()
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
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
                color = com.lojasocial.app.ui.submitApplications.components.TextGray
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

/**
 * Utility function to extract the filename from a URI.
 * 
 * This function attempts to retrieve the display name of a file from its URI.
 * It first tries to get the filename from the ContentResolver using the
 * OpenableColumns.DISPLAY_NAME column. If that fails, it falls back to
 * extracting the filename from the URI path.
 * 
 * @param context Android context for accessing ContentResolver
 * @param uri The URI of the file to get the filename from
 * @return The filename if found, or "Ficheiro sem nome" as a fallback
 */
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
        CandidaturaDocumentsView(onNavigateBack = {}, onSubmit = {}, viewModel = hiltViewModel())
    }
}