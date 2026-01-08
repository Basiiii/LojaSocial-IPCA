package com.lojasocial.app.ui.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.lojasocial.app.domain.request.RequestStatus
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.ui.components.RequestItemCard
import com.lojasocial.app.ui.components.StatusTabSelector
import com.lojasocial.app.ui.requests.components.RequestDetailsDialog
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupRequestsView(
    onNavigateBack: () -> Unit,
    viewModel: PickupRequestsViewModel = hiltViewModel(),
    userRepository: UserRepository? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Check if user is admin
    var isAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (userRepository != null) {
            val profile = userRepository.getCurrentUserProfile().firstOrNull()
            isAdmin = profile?.isAdmin == true
        }
    }
    
    // Tab selection state
    var selectedTab by remember { mutableStateOf(RequestStatus.SUBMETIDO.label) }
    
    // Get all status labels for tabs, excluding CANCELADO for admin users
    val tabOptions = remember(isAdmin) {
        RequestStatus.values()
            .filter { if (isAdmin) it != RequestStatus.CANCELADO else true }
            .map { it.label }
    }

    // Handle action state messages with Snackbar
    LaunchedEffect(actionState) {
        val currentActionState = actionState
        when (currentActionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar(
                    message = currentActionState.message,
                    duration = SnackbarDuration.Short
                )
                // Close dialog after showing success message
                viewModel.clearSelectedRequest()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar(
                    message = currentActionState.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    // Show dialog when request is selected
    selectedRequest?.let { request ->
        RequestDetailsDialog(
            request = request,
            userName = userProfile?.name ?: "",
            userEmail = userProfile?.email ?: "",
            isLoading = actionState is ActionState.Loading,
            onDismiss = {
                viewModel.clearSelectedRequest()
                viewModel.resetActionState()
            },
            onAccept = { date ->
                viewModel.acceptRequest(request.id, date)
            },
            onReject = { reason ->
                viewModel.rejectRequest(request.id, reason)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pedidos de Levantamento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            val currentUiState = uiState

            // --- Info Bar (Gray Strip) ---
            when (currentUiState) {
                is PickupRequestsUiState.Success -> {
                    val pendingCount = currentUiState.requests.count { it.status == 0 } // SUBMETIDO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6)) // Light Gray bg
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pending Count with Orange Dot
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B)) // Orange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$pendingCount Pendente${if (pendingCount != 1) "s" else ""}",
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )
                        }

                        // Update Time
                        Text(
                            text = "Atualizado agora",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    // Empty info bar for loading/error states
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            // --- Divider ---
            Divider(color = Color(0xFFE5E7EB))
            
            // --- Status Tabs ---
            when (currentUiState) {
                is PickupRequestsUiState.Success -> {
                    StatusTabSelector(
                        options = tabOptions,
                        selectedOption = selectedTab,
                        onOptionSelected = { newTab ->
                            selectedTab = newTab
                        }
                    )
                }
                else -> {}
            }

            // --- List of Requests ---
            when (currentUiState) {
                is PickupRequestsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PickupRequestsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Erro ao carregar pedidos",
                                color = Color(0xFFDC2626),
                                fontSize = 16.sp
                            )
                            Text(
                                text = currentUiState.message,
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                            Button(onClick = { viewModel.fetchRequests() }) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }
                is PickupRequestsUiState.Success -> {
                    // Filter requests based on selected tab
                    val selectedStatus = RequestStatus.values().find { it.label == selectedTab } ?: RequestStatus.SUBMETIDO
                    val statusInt = when (selectedStatus) {
                        RequestStatus.SUBMETIDO -> 0
                        RequestStatus.PENDENTE_LEVANTAMENTO -> 1
                        RequestStatus.CONCLUIDO -> 2
                        RequestStatus.REJEITADO -> 4  // Fixed: REJEITADO is 4, not 3
                        RequestStatus.CANCELADO -> 3   // Fixed: CANCELADO is 3, not 4
                    }
                    
                    val filteredRequests = currentUiState.requests.filter { it.status == statusInt }
                    
                    if (filteredRequests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Não há pedidos ${selectedStatus.label.lowercase()}",
                                color = Color(0xFF6B7280),
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn {
                            items(filteredRequests.size) { index ->
                                val request = filteredRequests[index]
                                
                                // Convert status int to RequestStatus enum
                                val status = when (request.status) {
                                    0 -> RequestStatus.SUBMETIDO
                                    1 -> RequestStatus.PENDENTE_LEVANTAMENTO
                                    2 -> RequestStatus.CONCLUIDO
                                    3 -> RequestStatus.CANCELADO  // Fixed: 3 is CANCELADO
                                    4 -> RequestStatus.REJEITADO  // Fixed: 4 is REJEITADO
                                    else -> RequestStatus.SUBMETIDO
                                }

                                // Format time ago
                                val timeAgo = request.submissionDate?.let { date ->
                                    val now = Date()
                                    val diff = now.time - date.time
                                    val seconds = diff / 1000
                                    val minutes = seconds / 60
                                    val hours = minutes / 60
                                    val days = hours / 24
                                    
                                    when {
                                        days > 0 -> "há $days dia${if (days > 1) "s" else ""}"
                                        hours > 0 -> "há $hours hora${if (hours > 1) "s" else ""}"
                                        minutes > 0 -> "há $minutes minuto${if (minutes > 1) "s" else ""}"
                                        else -> "agora"
                                    }
                                } ?: ""

                                RequestItemCard(
                                    status = status,
                                    title = status.label,
                                    subtitle = when (status) {
                                        RequestStatus.SUBMETIDO -> "Pedido submetido e pendente"
                                        RequestStatus.PENDENTE_LEVANTAMENTO -> "O teu pedido foi aceite"
                                        RequestStatus.CONCLUIDO -> "Levantamento feito com sucesso"
                                        RequestStatus.REJEITADO -> "O teu pedido não foi aceite. Clique para ver detalhes"
                                        RequestStatus.CANCELADO -> "Cancelaste o teu pedido."
                                    },
                                    time = timeAgo,
                                    onClick = {
                                        viewModel.selectRequest(request.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
