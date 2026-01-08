package com.lojasocial.app.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.application.Application
import com.lojasocial.app.domain.application.ApplicationStatus
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import java.util.Date
import java.util.concurrent.TimeUnit

// Modelo de dados simples para representar o item da lista
data class CandidaturaItemUi(
    val id: String,
    val nome: String,
    val estado: String, // ex: "Pendente", "Aprovado", "Rejeitado"
    val tempoAtras: String,
    val isOwnApplication: Boolean = false // True if this is the current user's own application
)

/**
 * Formats a date to a "time ago" string in Portuguese.
 * Examples: "5h atrás", "2d atrás", "1 semana atrás"
 */
fun formatTimeAgo(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
    val weeks = days / 7
    val months = days / 30
    
    return when {
        minutes < 1 -> "Agora"
        minutes < 60 -> "${minutes}min atrás"
        hours < 24 -> "${hours}h atrás"
        days < 7 -> "${days}d atrás"
        weeks < 4 -> "${weeks} semana${if (weeks > 1) "s" else ""} atrás"
        months < 12 -> "${months} mês${if (months > 1) "es" else ""} atrás"
        else -> "${months / 12} ano${if (months / 12 > 1) "s" else ""} atrás"
    }
}

/**
 * Converts ApplicationStatus enum to Portuguese label.
 */
fun ApplicationStatus.toPortugueseLabel(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "Pendente"
        ApplicationStatus.UNDER_REVIEW -> "Em Revisão"
        ApplicationStatus.APPROVED -> "Aprovado"
        ApplicationStatus.REJECTED -> "Rejeitado"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsListView(
    applicationRepository: ApplicationRepository,
    onNavigateBack: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    showAllApplications: Boolean = false, // If true, shows all applications regardless of userId
    currentUserId: String? = null, // Current user ID to identify own applications (for disabling interaction)
    isBeneficiary: Boolean = false, // If true, hides the add button (user is already a beneficiary)
    title: String? = null // Custom title. If null, uses default based on showAllApplications
) {
    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }

    // Fetch applications from repository
    LaunchedEffect(showAllApplications) {
        val applicationsFlow = if (showAllApplications) {
            applicationRepository.getAllApplications()
        } else {
            applicationRepository.getApplications()
        }
        
        applicationsFlow.collect { apps ->
            applications = apps
            isLoading = false
        }
    }

    // Filter applications by selected status (no longer excluding current user's applications)
    var filteredApplications = if (selectedStatusFilter != null) {
        applications.filter { it.status == selectedStatusFilter }
    } else {
        applications
    }

    // Map Application domain objects to UI model and sort by date (most recent first)
    val candidaturas = filteredApplications
        .sortedByDescending { it.submissionDate } // Sort by submission date, most recent first
        .map { app ->
            CandidaturaItemUi(
                id = app.id,
                nome = app.personalInfo.name,
                estado = app.status.toPortugueseLabel(),
                tempoAtras = formatTimeAgo(app.submissionDate),
                isOwnApplication = currentUserId != null && app.userId == currentUserId
            )
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        title ?: if (showAllApplications) "Todas as Candidaturas" else "Candidaturas",
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
                actions = {
                    // Only show add button if not showing all applications (employee view) and user is not already a beneficiary
                    if (!showAllApplications && !isBeneficiary) {
                        IconButton(onClick = onAddClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar",
                                tint = Color.DarkGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White // Fundo geral branco
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    // Application Status Tab Selector - always visible when not loading
                    ApplicationStatusTab(
                        selectedStatus = selectedStatusFilter,
                        onStatusSelected = { status ->
                            selectedStatusFilter = status
                        }
                    )
                    
                    // Applications List or Empty State
                    if (candidaturas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhuma candidatura encontrada",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        // Applications List
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(candidaturas) { candidatura ->
                                ApplicationListItem(
                                    item = candidatura,
                                    onClick = { 
                                        // Only allow click if it's not the user's own application
                                        if (!candidatura.isOwnApplication) {
                                            onItemClick(candidatura.id)
                                        }
                                    },
                                    enabled = !candidatura.isOwnApplication
                                )
                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationListItem(
    item: CandidaturaItemUi,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .then(if (!enabled) Modifier.alpha(0.6f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar (Imagem de Perfil)
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.LightGray
        ) {
            // Aqui podes usar AsyncImage (Coil) se tiveres URL.
            // Para já, uso um Ícone como placeholder ou uma cor.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Color.DarkGray)) {
                // Se quiseres simular a foto, podes por uma Image aqui
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Coluna Central (Nome e Estado)
        Column(
            modifier = Modifier.weight(1f) // Ocupa o espaço disponível
        ) {
            Text(
                text = item.nome,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Color.Black else Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Chip de Estado "Pendente"
            StatusChip(status = item.estado)
        }

        // 3. Coluna Direita (Tempo e Seta/Indicador)
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.tempoAtras,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (enabled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Ver detalhes",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (!enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A tua candidatura",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    // Cores e ícones baseados no status
    val (bgColor, contentColor, icon) = when (status) {
        "Pendente" -> Triple(
            Color(0xFFFBE9E7), 
            Color(0xFFBF360C), 
            Icons.Default.AccessTime
        ) // Laranja/Castanho
        "Aprovado" -> Triple(
            Color(0xFFE8F5E9), 
            Color(0xFF2E7D32), 
            Icons.Default.CheckCircle
        ) // Verde
        "Rejeitado" -> Triple(
            Color(0xFFFFEBEE), 
            Color(0xFFC62828), 
            Icons.Default.Cancel
        ) // Vermelho
        "Em Revisão" -> Triple(
            Color(0xFFE3F2FD), 
            Color(0xFF1976D2), 
            Icons.Default.AccessTime
        ) // Azul
        else -> Triple(
            Color.LightGray, 
            Color.Black, 
            Icons.Default.AccessTime
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ApplicationsListPreview() {
    MaterialTheme {
        // Preview with mock repository would require creating a mock, 
        // but for now we'll just show the structure
        Text("Preview - ApplicationsListView")
    }
}