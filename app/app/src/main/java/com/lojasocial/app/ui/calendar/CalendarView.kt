package com.lojasocial.app.ui.calendar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.request.PickupRequest
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
            .background(LojaSocialBackground)
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
                // Not used for PENDENTE_LEVANTAMENTO status in calendar
                viewModel.clearSelectedRequest()
            },
            onReject = { reason ->
                // Not used for PENDENTE_LEVANTAMENTO status in calendar
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
            onAcceptEmployeeDate = {
                // Not used in calendar context
            },
            onProposeNewDeliveryDate = { date ->
                // Not used in calendar context
            },
            currentUserId = currentUserId,
            onRescheduleDelivery = { date ->
                // Determine if employee or beneficiary is rescheduling based on portal context
                viewModel.rescheduleDelivery(request.id, date, isEmployeeRescheduling = !isBeneficiaryPortal)
            }
        )
    }
}

/**
 * Header showing current month with navigation buttons.
 */
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

/**
 * Calendar grid showing days of the month.
 */
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
    
    // Day names header
    val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    
    Column {
        // Day names row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEach { dayName ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
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
        
        // Calendar days grid
        var dayCounter = 1
        val weeks = mutableListOf<List<Int?>>()
        var currentWeek = mutableListOf<Int?>()
        
        // Add empty cells for days before the first day of the month
        for (i in 1 until firstDayOfWeek) {
            currentWeek.add(null)
        }
        
        // Add days of the month
        while (dayCounter <= daysInMonth) {
            currentWeek.add(dayCounter)
            dayCounter++
            
            if (currentWeek.size == 7) {
                weeks.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }
        
        // Add remaining empty cells
        while (currentWeek.size < 7 && currentWeek.isNotEmpty()) {
            currentWeek.add(null)
        }
        if (currentWeek.isNotEmpty()) {
            weeks.add(currentWeek)
        }
        
        // Render weeks
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

/**
 * Individual day cell in the calendar.
 */
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
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(4.dp)
        )
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
        
        // Normalize date for lookup (remove time component)
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
        
        // Check if any accepted requests are concluded (status == 2)
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
                        // Half-half background when both exist
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.background(
                            when {
                                hasCampaigns -> Color(0xFFFFE5B4) // Light orange/amber for campaign days
                                hasConcludedRequests -> Color(0xFFD1FAE5) // Light green for concluded deliveries
                                hasAcceptedRequests -> Color(0xFFE0F2FE) // Light blue for accepted request days
                                isToday -> LojaSocialPrimary.copy(alpha = 0.1f)
                                else -> LojaSocialSurface
                            }
                        )
                    }
                )
                .clickable { onDateSelected(date) },
            contentAlignment = Alignment.Center
        ) {
            // Half-half background when both campaign and accepted request exist
            if (hasBoth && !isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    // Left half - Campaign color
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFFFFE5B4))
                    )
                    // Right half - Accepted request color (green if concluded, blue otherwise)
                    val rightHalfColor = if (dateAcceptedRequests.any { it.status == 2 }) {
                        Color(0xFFD1FAE5) // Light green for concluded
                    } else {
                        Color(0xFFE0F2FE) // Light blue for pending
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(rightHalfColor)
                    )
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
                        hasBoth -> TextDark // Use default text color when both exist (background provides visual distinction)
                        hasCampaigns -> Color(0xFFD97706) // Orange text for campaign days
                        hasConcludedRequests -> Color(0xFF059669) // Green text for concluded deliveries
                        hasAcceptedRequests -> Color(0xFF0284C7) // Blue text for accepted request days
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
                                .background(
                                    if (isSelected) LojaSocialOnPrimary
                                    else LojaSocialPrimary
                                )
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
                            hasConcludedRequests -> Color(0xFF059669) // Green for concluded
                            else -> Color(0xFF0284C7) // Blue for pending
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

/**
 * Section showing pickup requests for the selected date.
 */
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (requests.isEmpty() && acceptedRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum levantamento neste dia",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show completed pickup requests
                requests.forEach { request ->
                    PickupRequestItem(request = request)
                }
                
                // Show accepted requests (Pedido Aceite)
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

/**
 * Section showing campaigns for the selected date.
 */
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
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            campaigns.forEach { campaign ->
                CampaignItem(campaign = campaign)
            }
        }
    }
}

/**
 * Individual campaign item.
 */
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE5B4) // Light orange/amber background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(Color(0xFFD97706).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campaign.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$startDateStr - $endDateStr",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        }
    }
}

/**
 * Individual accepted request item.
 */
@Composable
private fun AcceptedRequestItem(
    request: Request,
    beneficiaryProfile: UserProfileData?,
    profilePictureBase64: String?,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
    val pickupDateStr = request.scheduledPickupDate?.let { dateFormatter.format(it) } ?: ""
    
    // Determine if request is concluded (status == 2)
    val isConcluded = request.status == 2
    val backgroundColor = if (isConcluded) {
        Color(0xFFD1FAE5) // Light green for concluded
    } else {
        Color(0xFFE0F2FE) // Light blue for pending
    }
    val iconBgColor = if (isConcluded) {
        Color(0xFF059669).copy(alpha = 0.2f) // Green tint
    } else {
        Color(0xFF0284C7).copy(alpha = 0.2f) // Blue tint
    }
    val iconTint = if (isConcluded) {
        Color(0xFF059669) // Green
    } else {
        Color(0xFF0284C7) // Blue
    }
    
    // Get beneficiary name
    val beneficiaryName = beneficiaryProfile?.name?.takeIf { it.isNotEmpty() } ?: "Utilizador"
    
    // Decode profile picture
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
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(iconBgColor),
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
                    // Show initials if no profile picture
                    Text(
                        text = beneficiaryName.take(2).uppercase(),
                        color = iconTint,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beneficiaryName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (pickupDateStr.isNotEmpty()) "Levantamento: $pickupDateStr" else "Data não definida",
                    fontSize = 14.sp,
                    color = TextGray
                )
                if (request.totalItems > 0) {
                    Text(
                        text = "${request.totalItems} item${if (request.totalItems != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

/**
 * Individual pickup request item.
 */
@Composable
private fun PickupRequestItem(
    request: PickupRequest
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LojaSocialSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(AppBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = LojaSocialPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.userName ?: "Utilizador",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${request.totalItems} item${if (request.totalItems != 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        }
    }
}

