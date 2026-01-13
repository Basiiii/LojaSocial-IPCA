package com.lojasocial.app.ui.calendar

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
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
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.domain.request.PickupRequest
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.request.UserProfileData
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.ui.requests.components.RequestDetailsDialog
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main calendar view that displays a calendar and pickup requests for selected dates.
 */
@Composable
fun CalendarView(
    viewModel: CalendarViewModel = hiltViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    isBeneficiaryPortal: Boolean = false,
    profilePictureRepository: ProfilePictureRepository? = null
) {
    val productRepository = viewModel.productRepository
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val pickupRequests by viewModel.pickupRequests.collectAsState()
    val pickupCounts by viewModel.pickupCounts.collectAsState()
    val campaignsByDate by viewModel.campaignsByDate.collectAsState()
    val acceptedRequestsByDate by viewModel.acceptedRequestsByDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoadingRequest by viewModel.isLoadingRequest.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val beneficiaryProfiles by viewModel.beneficiaryProfiles.collectAsState()
    val profilePictures by viewModel.profilePictures.collectAsState()

    // Update ViewModel with portal context
    LaunchedEffect(isBeneficiaryPortal) {
        viewModel.setBeneficiaryPortalContext(isBeneficiaryPortal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Light grey background
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Month navigation header
        MonthHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { viewModel.navigateToPreviousMonth() },
            onNextMonth = { viewModel.navigateToNextMonth() },
            onToday = { viewModel.navigateToToday() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            pickupCounts = pickupCounts,
            campaignsByDate = campaignsByDate,
            acceptedRequestsByDate = acceptedRequestsByDate,
            onDateSelected = { date ->
                viewModel.selectDate(date)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pickup requests and campaigns list
        if (selectedDate != null) {
            Column {
                // Normalize date for lookup
                val normalizedDate = Calendar.getInstance().apply {
                    time = selectedDate!!
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // Get accepted requests for this date
                val dateAcceptedRequests = acceptedRequestsByDate[normalizedDate] ?: emptyList()

                PickupRequestsSection(
                    date = selectedDate!!,
                    requests = pickupRequests,
                    acceptedRequests = dateAcceptedRequests,
                    isLoading = isLoading,
                    beneficiaryProfiles = beneficiaryProfiles,
                    profilePictures = profilePictures,
                    onRequestClick = { requestId ->
                        viewModel.selectRequest(requestId)
                    }
                )

                // Show campaigns for selected date
                val dateCampaigns = campaignsByDate[normalizedDate]
                if (!dateCampaigns.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CampaignsSection(
                        date = selectedDate!!,
                        campaigns = dateCampaigns
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Selecione uma data para ver os levantamentos",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
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
            isBeneficiaryView = isBeneficiaryPortal,
            onAcceptEmployeeDate = {},
            onProposeNewDeliveryDate = {},
            currentUserId = currentUserId,
            onRescheduleDelivery = { date ->
                viewModel.rescheduleDelivery(request.id, date, isEmployeeRescheduling = !isBeneficiaryPortal)
            }
        )
    }
}

@Composable
private fun MonthHeader(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT"))
    val monthText = monthFormatter.format(currentMonth.time).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Mês anterior",
                tint = LojaSocialPrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onToday() }
        ) {
            Text(
                text = monthText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Hoje",
                fontSize = 12.sp,
                color = LojaSocialPrimary
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Próximo mês",
                tint = LojaSocialPrimary
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Date?,
    pickupCounts: Map<Date, Int>,
    campaignsByDate: Map<Date, List<Campaign>>,
    acceptedRequestsByDate: Map<Date, List<Request>>,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance().apply { time = currentMonth.time }
    val firstDayOfMonth = Calendar.getInstance().apply {
        time = calendar.time
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEach { dayName ->
                Box(
                    modifier = Modifier.weight(1f).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var dayCounter = 1
        val weeks = mutableListOf<List<Int?>>()
        var currentWeek = mutableListOf<Int?>()

        for (i in 1 until firstDayOfWeek) {
            currentWeek.add(null)
        }

        while (dayCounter <= daysInMonth) {
            currentWeek.add(dayCounter)
            dayCounter++

            if (currentWeek.size == 7) {
                weeks.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }

        while (currentWeek.size < 7 && currentWeek.isNotEmpty()) {
            currentWeek.add(null)
        }
        if (currentWeek.isNotEmpty()) {
            weeks.add(currentWeek)
        }

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        currentMonth = calendar,
                        selectedDate = selectedDate,
                        pickupCounts = pickupCounts,
                        campaignsByDate = campaignsByDate,
                        acceptedRequestsByDate = acceptedRequestsByDate,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DayCell(
    day: Int?,
    currentMonth: Calendar,
    selectedDate: Date?,
    pickupCounts: Map<Date, Int>,
    campaignsByDate: Map<Date, List<Campaign>>,
    acceptedRequestsByDate: Map<Date, List<Request>>,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    if (day == null) {
        Box(modifier = modifier.aspectRatio(1f).padding(4.dp))
    } else {
        val date = Calendar.getInstance().apply {
            time = currentMonth.time
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val isSelected = selectedDate?.let {
            val selectedCal = Calendar.getInstance().apply { time = it }
            val dayCal = Calendar.getInstance().apply { time = date }
            selectedCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                    selectedCal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                    selectedCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
        } ?: false

        val isToday = Calendar.getInstance().let { today ->
            val dayCal = Calendar.getInstance().apply { time = date }
            today.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
        }

        val normalizedDate = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val pickupCount = pickupCounts[normalizedDate] ?: 0
        val dateCampaigns = campaignsByDate[normalizedDate] ?: emptyList()
        val dateAcceptedRequests = acceptedRequestsByDate[normalizedDate] ?: emptyList()
        val hasCampaigns = dateCampaigns.isNotEmpty()
        val hasAcceptedRequests = dateAcceptedRequests.isNotEmpty()
        val hasBoth = hasCampaigns && hasAcceptedRequests
        val hasConcludedRequests = dateAcceptedRequests.any { it.status == 2 }

        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(LojaSocialPrimary)
                    } else if (hasBoth) {
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.background(
                            when {
                                hasCampaigns -> Color(0xFFFFE5B4)
                                hasConcludedRequests -> Color(0xFFD1FAE5)
                                hasAcceptedRequests -> Color(0xFFE0F2FE)
                                isToday -> LojaSocialPrimary.copy(alpha = 0.1f)
                                else -> LojaSocialSurface
                            }
                        )
                    }
                )
                .clickable { onDateSelected(date) },
            contentAlignment = Alignment.Center
        ) {
            if (hasBoth && !isSelected) {
                Row(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFFE5B4)))
                    val rightHalfColor = if (dateAcceptedRequests.any { it.status == 2 }) {
                        Color(0xFFD1FAE5)
                    } else {
                        Color(0xFFE0F2FE)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(rightHalfColor))
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = day.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || isToday || hasCampaigns || hasAcceptedRequests) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> LojaSocialOnPrimary
                        hasBoth -> TextDark
                        hasCampaigns -> Color(0xFFD97706)
                        hasConcludedRequests -> Color(0xFF059669)
                        hasAcceptedRequests -> Color(0xFF0284C7)
                        isToday -> LojaSocialPrimary
                        else -> TextDark
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (pickupCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) LojaSocialOnPrimary else LojaSocialPrimary)
                        )
                    }
                    if (hasCampaigns) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Campanha",
                            modifier = Modifier.size(8.dp),
                            tint = if (isSelected) LojaSocialOnPrimary else Color(0xFFD97706)
                        )
                    }
                    if (hasAcceptedRequests) {
                        val iconColor = when {
                            isSelected -> LojaSocialOnPrimary
                            hasConcludedRequests -> Color(0xFF059669)
                            else -> Color(0xFF0284C7)
                        }
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Pedido Aceite",
                            modifier = Modifier.size(8.dp),
                            tint = iconColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickupRequestsSection(
    date: Date,
    requests: List<PickupRequest>,
    acceptedRequests: List<Request>,
    isLoading: Boolean,
    beneficiaryProfiles: Map<String, UserProfileData>,
    profilePictures: Map<String, String?>,
    onRequestClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
    val formattedDate = dateFormatter.format(date).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    Column {
        Text(
            text = "Levantamentos - $formattedDate",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (requests.isEmpty() && acceptedRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum levantamento neste dia",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                requests.forEach { request ->
                    PickupRequestItem(request = request)
                }
                acceptedRequests.forEach { request ->
                    AcceptedRequestItem(
                        request = request,
                        beneficiaryProfile = beneficiaryProfiles[request.id],
                        profilePictureBase64 = profilePictures[request.id],
                        onClick = { onRequestClick(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CampaignsSection(
    date: Date,
    campaigns: List<Campaign>
) {
    val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
    val formattedDate = dateFormatter.format(date).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    Column {
        Text(
            text = "Campanhas - $formattedDate",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            campaigns.forEach { campaign ->
                CampaignItem(campaign = campaign)
            }
        }
    }
}

@Composable
private fun CampaignItem(
    campaign: Campaign
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
    val startDateStr = dateFormatter.format(campaign.startDate)
    val endDateStr = dateFormatter.format(campaign.endDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF7ED)), // Very light orange
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color(0xFFC2410C), // Dark Orange
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campaign.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$startDateStr - $endDateStr",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

/**
 * Modern, clean Card design for Accepted Requests.
 */
@Composable
private fun AcceptedRequestItem(
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

    // Status Badge colors
    val statusBgColor = if (isConcluded) Color(0xFFDCFCE7) else Color(0xFFE0F2FE) // Light Green vs Light Blue
    val statusTextColor = if (isConcluded) Color(0xFF166534) else Color(0xFF0369A1) // Dark Green vs Dark Blue
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)) // Subtle border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
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

            // Main Info
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
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge (Pill)
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

/**
 * Individual pickup request item (Simplified for read-only look).
 */
@Composable
private fun PickupRequestItem(
    request: PickupRequest
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(LojaSocialPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = LojaSocialPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.userName ?: "Utilizador",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${request.totalItems} item${if (request.totalItems != 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
        }
    }
}