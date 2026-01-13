package com.lojasocial.app.ui.weeklypickups

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.ui.requests.components.RequestDetailsDialog
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPickupsView(
    viewModel: WeeklyPickupsViewModel = hiltViewModel(),
    profilePictureRepository: ProfilePictureRepository? = null,
    onNavigateBack: () -> Unit = {}
) {
    val productRepository = viewModel.productRepository
    val weeklyPickups by viewModel.weeklyPickups.collectAsState()
    val beneficiaryProfiles by viewModel.beneficiaryProfiles.collectAsState()
    val profilePictures by viewModel.profilePictures.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoadingRequest by viewModel.isLoadingRequest.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Agendamentos da Semana",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (weeklyPickups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextGray
                        )
                        Text(
                            text = "Nenhum levantamento agendado para esta semana",
                            fontSize = 16.sp,
                            color = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(weeklyPickups) { request ->
                        WeeklyPickupCard(
                            request = request,
                            beneficiaryProfile = beneficiaryProfiles[request.id],
                            profilePictureBase64 = profilePictures[request.id],
                            onClick = { viewModel.selectRequest(request.id) }
                        )
                    }
                }
            }
        }
    }

    // Show request details dialog when a request is selected
    selectedRequest?.let { request ->
        RequestDetailsDialog(
            request = request,
            userName = userProfile?.name ?: "",
            userEmail = userProfile?.email ?: "",
            isLoading = isLoadingRequest,
            onDismiss = {
                viewModel.clearSelectedRequest()
            },
            onAccept = { date ->
                viewModel.clearSelectedRequest()
            },
            onReject = { reason ->
                viewModel.clearSelectedRequest()
            },
            onComplete = {
                viewModel.completeRequest(request.id)
            },
            onCancelDelivery = { beneficiaryAbsent ->
                viewModel.cancelDelivery(request.id, beneficiaryAbsent)
            },
            profilePictureRepository = profilePictureRepository,
            productRepository = productRepository,
            isBeneficiaryView = false,
            onAcceptEmployeeDate = {},
            onProposeNewDeliveryDate = {},
            currentUserId = currentUserId,
            onRescheduleDelivery = { date ->
                viewModel.rescheduleDelivery(request.id, date, isEmployeeRescheduling = true)
            }
        )
    }
}

@Composable
private fun WeeklyPickupCard(
    request: Request,
    beneficiaryProfile: UserProfileData?,
    profilePictureBase64: String?,
    onClick: () -> Unit
) {
    val requestCategory = when {
        request.items.isEmpty() -> "Vários"
        else -> {
            val firstItemCategory = request.items.firstOrNull()?.category ?: 1
            when (ProductCategory.fromId(firstItemCategory)) {
                ProductCategory.ALIMENTAR -> "Alimentar"
                ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                ProductCategory.CASA -> "Limpeza"
                null -> "Vários"
            }
        }
    }

    val isConcluded = request.status == 2
    val statusBgColor = if (isConcluded) Color(0xFFDCFCE7) else Color(0xFFE0F2FE)
    val statusTextColor = if (isConcluded) Color(0xFF166534) else Color(0xFF0369A1)
    val statusText = if (isConcluded) "Concluído" else "Pendente"
    val statusIcon = if (isConcluded) Icons.Default.CheckCircle else Icons.Default.Schedule

    val beneficiaryName = beneficiaryProfile?.name?.takeIf { it.isNotEmpty() } ?: "Utilizador"

    var imageBitmap by remember(profilePictureBase64) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(profilePictureBase64) {
        profilePictureBase64?.let { base64 ->
            if (base64.isNotBlank()) {
                imageBitmap = withContext(Dispatchers.IO) {
                    try {
                        val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                        bytes?.let {
                            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } ?: run {
            imageBitmap = null
        }
    }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
    val pickupDateStr = request.scheduledPickupDate?.let { dateFormatter.format(it) } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = beneficiaryName.take(2).uppercase(),
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beneficiaryName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(requestCategory),
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$requestCategory • ${request.totalItems} items",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
                if (pickupDateStr.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pickupDateStr,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = statusBgColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        color = statusTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "limpeza" -> Icons.Default.CleaningServices
        "alimentar" -> Icons.Default.Restaurant
        "higiene" -> Icons.Default.Spa
        else -> Icons.Default.ShoppingCart
    }
}
