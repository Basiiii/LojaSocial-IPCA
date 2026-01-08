package com.lojasocial.app.ui.requests

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.request.RequestStatus
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.ui.components.StatusTabSelector
import com.lojasocial.app.ui.requests.components.RequestDetailsDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupRequestsView(
    onNavigateBack: () -> Unit,
    viewModel: PickupRequestsViewModel = hiltViewModel(),
    userRepository: UserRepository? = null,
    requestsRepository: RequestsRepository? = null,
    profilePictureRepository: ProfilePictureRepository? = null,
    filterByCurrentUser: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Cache for user profiles by userId
    var userProfilesCache by remember { mutableStateOf<Map<String, UserProfileData>>(emptyMap()) }
    
    // Check if user is admin and get current user ID
    var isAdmin by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        if (userRepository != null) {
            val profile = userRepository.getCurrentUserProfile().firstOrNull()
            isAdmin = profile?.isAdmin == true
            currentUserId = profile?.uid
        }
    }
    
    // Filter by current user if requested (for beneficiary portal)
    LaunchedEffect(filterByCurrentUser, currentUserId) {
        if (filterByCurrentUser && currentUserId != null) {
            viewModel.setFilterUserId(currentUserId)
        } else if (!filterByCurrentUser) {
            viewModel.setFilterUserId(null)
        }
    }
    
    // Tab selection state
    var selectedTab by remember { mutableStateOf(RequestStatus.SUBMETIDO.label) }
    
    // Get all status labels for tabs
    // For beneficiaries viewing their own requests, show all statuses including CANCELADO
    // For admin users viewing all requests, exclude CANCELADO
    val tabOptions = remember(isAdmin, filterByCurrentUser) {
        RequestStatus.values()
            .filter { 
                // If admin viewing all requests, exclude CANCELADO
                // If beneficiary viewing own requests, show all including CANCELADO
                if (isAdmin && !filterByCurrentUser) {
                    it != RequestStatus.CANCELADO
                } else {
                    true
                }
            }
            .map { it.label }
    }

    // Fetch user profiles for all requests
    LaunchedEffect(uiState, requestsRepository) {
        if (uiState is PickupRequestsUiState.Success && requestsRepository != null) {
            val requests = (uiState as PickupRequestsUiState.Success).requests
            val uniqueUserIds = requests.map { it.userId }.distinct()
            
            // Fetch profiles for users not in cache
            val newProfiles = mutableMapOf<String, UserProfileData>()
            uniqueUserIds.forEach { userId ->
                if (!userProfilesCache.containsKey(userId)) {
                    val result = requestsRepository.getUserProfile(userId)
                    result.fold(
                        onSuccess = { profile -> newProfiles[userId] = profile },
                        onFailure = { newProfiles[userId] = UserProfileData() }
                    )
                }
            }
            
            if (newProfiles.isNotEmpty()) {
                userProfilesCache = userProfilesCache + newProfiles
            }
        }
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
            },
            profilePictureRepository = profilePictureRepository,
            canAcceptReject = !filterByCurrentUser // Beneficiaries can't accept/reject their own requests
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (filterByCurrentUser) "Os Meus Pedidos" else "Pedidos Pendentes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)
        ) {
            val currentUiState = uiState

            // --- Status Header (Blue strip) ---
            when (currentUiState) {
                is PickupRequestsUiState.Success -> {
                    val pendingCount = currentUiState.requests.count { it.status == 0 } // SUBMETIDO
                    StatusHeader(pendingCount = pendingCount)
                }
                else -> {
                    // Empty info bar for loading/error states
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEEF4F8))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

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
                        RequestStatus.REJEITADO -> 4
                        RequestStatus.CANCELADO -> 3
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
                            items(filteredRequests) { request ->
                                val userName = userProfilesCache[request.userId]?.name ?: "Utilizador"
                                val category = getRequestCategory(request)
                                val categoryIcon = getCategoryIcon(category)
                                
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
                                
                                // Convert status int to RequestStatus enum
                                val status = when (request.status) {
                                    0 -> RequestStatus.SUBMETIDO
                                    1 -> RequestStatus.PENDENTE_LEVANTAMENTO
                                    2 -> RequestStatus.CONCLUIDO
                                    3 -> RequestStatus.CANCELADO
                                    4 -> RequestStatus.REJEITADO
                                    else -> RequestStatus.SUBMETIDO
                                }
                                
                                PedidoItem(
                                    userId = request.userId,
                                    name = userName,
                                    timeAgo = timeAgo,
                                    category = category,
                                    categoryIcon = categoryIcon,
                                    status = status,
                                    onClick = {
                                        viewModel.selectRequest(request.id)
                                    },
                                    profilePictureRepository = profilePictureRepository
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = Color.LightGray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusHeader(pendingCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEF4F8)) // Light Blue
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Orange Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$pendingCount Pendente${if (pendingCount != 1) "s" else ""}",
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "Atualizado agora",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PedidoItem(
    userId: String,
    name: String,
    timeAgo: String,
    category: String,
    categoryIcon: ImageVector,
    status: RequestStatus,
    onClick: () -> Unit,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Fetch profile picture
    LaunchedEffect(userId, profilePictureRepository) {
        if (profilePictureRepository != null) {
            try {
                profilePictureRepository.getProfilePicture(userId)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (Profile picture or initials)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = name.take(2).uppercase(),
                    color = Color(0xFF6B7280),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Middle Content (Name, Badge, Category)
        Column(modifier = Modifier.weight(1f)) {
            // Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                // Time (Aligned to right of name)
                Text(
                    text = timeAgo,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Badges Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Badge - using status-specific icon and colors
                Surface(
                    color = status.iconBgColor.copy(alpha = 0.2f), // Use status background color with transparency
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = status.icon,
                            contentDescription = null,
                            tint = status.iconTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = status.label,
                            color = status.iconTint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Category (Icon + Text)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = category,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // The Arrow on the far right
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Details",
            tint = Color.Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Helper function to determine category from request items
fun getRequestCategory(request: Request): String {
    return when {
        request.items.isEmpty() -> "Vários"
        request.items.size > 1 -> "Vários"
        else -> {
            // Try to infer from first item name or use default
            val firstItemName = request.items.firstOrNull()?.productName?.lowercase() ?: ""
            when {
                firstItemName.contains("limpeza") || firstItemName.contains("detergente") -> "Limpeza"
                firstItemName.contains("comida") || firstItemName.contains("alimento") -> "Alimentar"
                firstItemName.contains("higiene") || firstItemName.contains("sabonete") -> "Higiene"
                else -> "Vários"
            }
        }
    }
}

// Helper function to get category icon
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "limpeza" -> Icons.Default.CleaningServices
        "alimentar" -> Icons.Default.Restaurant
        "higiene" -> Icons.Default.Spa
        else -> Icons.Default.Work // Default for "Vários"
    }
}
