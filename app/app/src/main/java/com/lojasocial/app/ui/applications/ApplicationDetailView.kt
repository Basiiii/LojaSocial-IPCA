package com.lojasocial.app.ui.applications

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.data.model.Application
import com.lojasocial.app.data.model.ApplicationStatus
import com.lojasocial.app.repository.ApplicationRepository
import com.lojasocial.app.ui.applications.formatTimeAgo
import com.lojasocial.app.ui.applications.StatusChip
import com.lojasocial.app.utils.FileUtils
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.lojasocial.app.data.model.ApplicationDocument
import com.lojasocial.app.ui.theme.LojaSocialPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailView(
    applicationId: String,
    applicationRepository: ApplicationRepository,
    onNavigateBack: () -> Unit = {},
    isEmployeeView: Boolean = false // If true, uses getApplicationByIdForEmployee
) {
    var application by remember { mutableStateOf<Application?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(applicationId, isEmployeeView, refreshTrigger) {
        try {
            isLoading = true
            error = null
            val result = if (isEmployeeView) {
                applicationRepository.getApplicationByIdForEmployee(applicationId)
            } else {
                applicationRepository.getApplicationById(applicationId)
            }
            if (result.isSuccess) {
                application = result.getOrNull()
                isLoading = false
            } else {
                error = result.exceptionOrNull()?.message ?: "Erro ao carregar candidatura"
                isLoading = false
            }
        } catch (e: Exception) {
            error = e.message ?: "Erro desconhecido"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Detalhes da Candidatura",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "Erro desconhecido",
                            color = Color.Red
                        )
                    }
                }
            }
            application != null -> {
                ApplicationDetailContent(
                    application = application!!,
                    context = context,
                    modifier = Modifier.padding(paddingValues),
                    isEmployeeView = isEmployeeView,
                    applicationRepository = applicationRepository,
                    onStatusUpdated = {
                        // Reload the application after status update
                        refreshTrigger++
                    }
                )
            }
        }
    }
}

@Composable
fun ApplicationDetailContent(
    application: Application,
    context: Context,
    modifier: Modifier = Modifier,
    isEmployeeView: Boolean = false,
    applicationRepository: ApplicationRepository? = null,
    onStatusUpdated: () -> Unit = {}
) {
    var showRejectionDialog by remember { mutableStateOf(false) }
    var rejectionMessage by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status and Date Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    StatusChip(status = application.status.toPortugueseLabel())
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data de Submissão",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = formatTimeAgo(application.submissionDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                Text(
                    text = dateFormat.format(application.submissionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Information Section
        InformationSection(
            title = "Informações Pessoais",
            items = listOf(
                "Nome" to application.personalInfo.name,
                "Email" to application.personalInfo.email,
                "Telefone" to application.personalInfo.phone,
                "BI/Passaporte" to application.personalInfo.idPassport,
                "Data de Nascimento" to (application.personalInfo.dateOfBirth?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "Não especificado")
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Academic Information Section
        InformationSection(
            title = "Informações Académicas",
            items = listOf(
                "Grau Académico" to application.academicInfo.academicDegree,
                "Curso" to application.academicInfo.course,
                "Número de Estudante" to application.academicInfo.studentNumber,
                "Apoio FAES" to if (application.academicInfo.faesSupport) "Sim" else "Não",
                "Tem Bolsa" to if (application.academicInfo.hasScholarship) "Sim" else "Não"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Documents Section
        DocumentsSection(
            documents = application.documents,
            context = context
        )

        // Rejection Message Section (if rejected)
        if (application.status == ApplicationStatus.REJECTED) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Mensagem do suporte",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (application.rejectionMessage != null && application.rejectionMessage.isNotBlank()) {
                        Text(
                            text = application.rejectionMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC62828)
                        )
                    } else {
                        Text(
                            text = "A tua candidatura foi rejeitada. Por favor, contacta o suporte para mais informações.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC62828),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Employee Action Buttons
        if (isEmployeeView && application.status != ApplicationStatus.APPROVED && application.status != ApplicationStatus.REJECTED) {
            Spacer(modifier = Modifier.height(24.dp))
            DecisionButtonsSection(
                onAcceptClick = {
                    isUpdating = true
                    updateError = null
                    coroutineScope.launch {
                        applicationRepository?.let { repo ->
                            val result = repo.updateApplicationStatus(
                                application.id,
                                ApplicationStatus.APPROVED
                            )
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Candidatura aceite com sucesso!",
                                    Toast.LENGTH_LONG
                                ).show()
                                onStatusUpdated()
                            } else {
                                updateError = result.exceptionOrNull()?.message ?: "Erro ao aprovar candidatura"
                                isUpdating = false
                            }
                        }
                    }
                },
                onRejectClick = { showRejectionDialog = true },
                acceptColor = LojaSocialPrimary,
                rejectColor = Color(0xFFC62828),
                isUpdating = isUpdating
            )
        }

        // Error message
        if (updateError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = updateError ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // Rejection Dialog
    if (showRejectionDialog) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = false },
            title = {
                Text("Rejeitar Candidatura")
            },
            text = {
                Column {
                    Text(
                        text = "Tens a certeza que queres rejeitar esta candidatura?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = rejectionMessage,
                        onValueChange = { rejectionMessage = it },
                        label = { Text("Mensagem (opcional)") },
                        placeholder = { Text("Explica o motivo da rejeição...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        updateError = null
                        coroutineScope.launch {
                            applicationRepository?.let { repo ->
                                val result = repo.updateApplicationStatus(
                                    application.id,
                                    ApplicationStatus.REJECTED,
                                    rejectionMessage.takeIf { it.isNotBlank() }
                                )
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "Candidatura rejeitada com sucesso!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showRejectionDialog = false
                                    rejectionMessage = ""
                                    onStatusUpdated()
                                } else {
                                    updateError = result.exceptionOrNull()?.message ?: "Erro ao rejeitar candidatura"
                                    isUpdating = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828)
                    ),
                    enabled = !isUpdating
                ) {
                    Text("Rejeitar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectionDialog = false },
                    enabled = !isUpdating
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DecisionButtonsSection(
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    acceptColor: Color,
    rejectColor: Color,
    isUpdating: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accept Button
        DecisionButton(
            text = "Aceitar Candidatura",
            icon = Icons.Default.Check,
            backgroundColor = acceptColor,
            onClick = onAcceptClick,
            enabled = !isUpdating
        )

        // Reject Button
        DecisionButton(
            text = "Rejeitar Candidatura",
            icon = Icons.Default.Close,
            backgroundColor = rejectColor,
            onClick = onRejectClick,
            enabled = !isUpdating
        )
    }
}

@Composable
fun DecisionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White,
            disabledContainerColor = backgroundColor.copy(alpha = 0.6f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InformationSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            items.forEach { (label, value) ->
                if (value.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                    if (label != items.last().first) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF5F5F5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentsSection(
    documents: List<ApplicationDocument>,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Documentos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (documents.isEmpty()) {
                Text(
                    text = "Nenhum documento anexado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                documents.forEach { document ->
                    DocumentItem(
                        document = document,
                        context = context,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (document != documents.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF5F5F5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItem(
    document: ApplicationDocument,
    context: Context,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (document.base64Data != null && document.base64Data!!.isNotBlank()) {
                    openDocumentFromBase64(context, document.base64Data!!, document.fileName ?: document.name)
                } else {
                    Toast.makeText(context, "Documento não disponível para visualização", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (document.fileName != null) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        if (document.base64Data != null) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Abrir documento",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun openDocumentFromBase64(context: Context, base64Data: String, fileName: String) {
    try {
        // Check if base64Data is not empty
        if (base64Data.isBlank()) {
            Toast.makeText(context, "Documento vazio ou não disponível", Toast.LENGTH_SHORT).show()
            android.util.Log.d("ApplicationDetailView", "Base64 data is blank")
            return
        }

        android.util.Log.d("ApplicationDetailView", "Attempting to open file: $fileName, base64 length: ${base64Data.length}")

        // Decode Base64 to bytes
        val fileBytes = FileUtils.convertBase64ToFile(base64Data).getOrNull()
        if (fileBytes == null || fileBytes.isEmpty()) {
            Toast.makeText(context, "Erro ao decodificar documento", Toast.LENGTH_SHORT).show()
            android.util.Log.e("ApplicationDetailView", "Failed to decode base64 data")
            return
        }

        android.util.Log.d("ApplicationDetailView", "Decoded ${fileBytes.size} bytes")

        // Create a temporary file with a unique name to avoid conflicts
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$sanitizedFileName")
        
        // Delete existing file if it exists
        if (tempFile.exists()) {
            tempFile.delete()
        }
        
        tempFile.createNewFile()

        // Write bytes to file
        FileOutputStream(tempFile).use { fos ->
            fos.write(fileBytes)
        }

        // Create URI using FileProvider (required for Android 7.0+)
        val uri = try {
            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            android.util.Log.d("ApplicationDetailView", "Created FileProvider URI: $fileProviderUri")
            fileProviderUri
        } catch (e: Exception) {
            android.util.Log.e("ApplicationDetailView", "Error creating FileProvider URI", e)
            Toast.makeText(context, "Erro ao criar URI do ficheiro: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // Determine MIME type from file extension
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = when (fileExtension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }

        // Open file with Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Grant permissions to all apps that can handle this intent
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        resolveInfos.forEach { resolveInfo ->
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: try to open with any app that can handle the file
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallbackIntent)
            } else {
                Toast.makeText(context, "Nenhuma aplicação disponível para abrir este tipo de ficheiro", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Erro ao abrir documento: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

