package com.lojasocial.app.ui.beneficiaries

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.application.Application
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.ui.applications.openDocumentFromBase64
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

// Define a modern color palette for this screen
private val BgColor = Color(0xFFF8F9FA)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)
private val AccentColor = Color(0xFF2563EB) // Royal Blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryDetailView(
    userId: String,
    userRepository: UserRepository,
    applicationRepository: ApplicationRepository,
    profilePictureRepository: ProfilePictureRepository? = null,
    onNavigateBack: () -> Unit = {}
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var application by remember { mutableStateOf<Application?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        try {
            isLoading = true
            error = null

            val profile = userRepository.getUserProfile(userId).first()
            userProfile = profile
            if (profile == null) {
                error = "Perfil de utilizador não encontrado"
                isLoading = false
                return@LaunchedEffect
            }

            val appResult = applicationRepository.getBeneficiaryApplication(userId)
            appResult.fold(
                onSuccess = { app ->
                    application = app
                    isLoading = false
                },
                onFailure = { e ->
                    error = e.message ?: "Erro ao carregar candidatura"
                    isLoading = false
                }
            )
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
                        "Detalhes do Beneficiário",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.Black
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardColor,
                    scrolledContainerColor = CardColor
                )
            )
        },
        containerColor = BgColor // Light gray background for contrast
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                }
                error != null -> {
                    ErrorState(error!!)
                }
                userProfile != null -> {
                    BeneficiaryDetailContent(
                        userProfile = userProfile!!,
                        application = application,
                        profilePictureRepository = profilePictureRepository
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BeneficiaryDetailContent(
    userProfile: UserProfile,
    application: Application?,
    profilePictureRepository: ProfilePictureRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing between cards
    ) {
        // 1. Profile Header (Clean & Centered)
        ProfileHeader(
            userProfile = userProfile,
            profilePictureRepository = profilePictureRepository
        )

        // 2. Application Sections
        if (application != null) {
            
            // Submission Metadata
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(application.submissionDate)
            
            InfoCard(title = "Resumo", icon = Icons.Outlined.Info) {
                 DetailRow(label = "Data de Submissão", value = formattedDate)
            }

            // Personal Info
            InfoCard(title = "Informações Pessoais", icon = Icons.Outlined.Person) {
                DetailRow(label = "Nome Completo", value = application.personalInfo.name)
                HorizontalDivider(color = BgColor)
                DetailRow(label = "Email", value = application.personalInfo.email)
                HorizontalDivider(color = BgColor)
                DetailRow(label = "Telefone", value = application.personalInfo.phone)
                HorizontalDivider(color = BgColor)
                DetailRow(label = "BI / Passaporte", value = application.personalInfo.idPassport)
                HorizontalDivider(color = BgColor)
                val birthDate = application.personalInfo.dateOfBirth?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "N/A"
                DetailRow(label = "Data de Nascimento", value = birthDate)
            }

            // Academic Info
            InfoCard(title = "Dados Académicos", icon = Icons.Outlined.School) {
                DetailRow(label = "Grau", value = application.academicInfo.academicDegree)
                HorizontalDivider(color = BgColor)
                DetailRow(label = "Curso", value = application.academicInfo.course)
                HorizontalDivider(color = BgColor)
                DetailRow(label = "Nº Estudante", value = application.academicInfo.studentNumber)
                
                // Boolean flags with visual chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(label = "Apoio FAES", isActive = application.academicInfo.faesSupport)
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusChip(label = "Bolseiro", isActive = application.academicInfo.hasScholarship)
                }
            }

            // Documents
            InfoCard(title = "Documentos", icon = Icons.Outlined.FolderOpen) {
                if (application.documents.isEmpty()) {
                    Text(
                        text = "Sem documentos anexados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    application.documents.forEachIndexed { index, doc ->
                        DocumentItem(
                            document = doc,
                            context = context,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (index < application.documents.size - 1) {
                            HorizontalDivider(
                                color = BgColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            EmptyStateCard()
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- Reusable Modern Components ---

@Composable
fun ProfileHeader(
    userProfile: UserProfile,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Fetch profile picture
    LaunchedEffect(userProfile.uid, profilePictureRepository) {
        if (profilePictureRepository != null) {
            try {
                profilePictureRepository.getProfilePicture(userProfile.uid)
                    .firstOrNull()
                    ?.let { base64 ->
                        // Decode Base64 to ImageBitmap
                        if (!base64.isNullOrBlank()) {
                            val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                            bytes?.let {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                imageBitmap = bitmap?.asImageBitmap()
                            }
                        }
                    }
            } catch (e: Exception) {
                // Handle error silently, fallback to initials
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Circle (Profile picture or initial)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (imageBitmap == null) Color(0xFFE0E7FF) else Color.Transparent), // Light Blue background if no image
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userProfile.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = userProfile.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = userProfile.email,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)) // Subtle border
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun StatusChip(label: String, isActive: Boolean) {
    val bgColor = if (isActive) Color(0xFFDCFCE7) else Color(0xFFF3F4F6) // Green vs Gray bg
    val textColor = if (isActive) Color(0xFF166534) else Color(0xFF6B7280) // Green vs Gray text
    val icon = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = if (isActive) "Sim" else "Não",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AssignmentLate,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhuma candidatura encontrada",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "Este utilizador ainda não tem processos ativos.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DocumentItem(
    document: com.lojasocial.app.domain.application.ApplicationDocument,
    context: Context,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (document.base64Data != null && document.base64Data!!.isNotBlank()) {
                    openDocumentFromBase64(context, document.base64Data!!, document.fileName ?: document.name)
                } else {
                    Toast.makeText(context, "Documento indisponível", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Updated icon color to match theme
        Surface(
            color = Color(0xFFEFF6FF), // Very light blue
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (document.fileName != null) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
        
        if (document.base64Data != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}
