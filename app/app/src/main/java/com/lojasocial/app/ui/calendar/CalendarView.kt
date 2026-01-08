package com.lojasocial.app.ui.calendar

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.request.PickupRequest
import com.lojasocial.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main calendar view that displays a calendar and pickup requests for selected dates.
 */
@Composable
fun CalendarView(
    viewModel: CalendarViewModel = hiltViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val pickupRequests by viewModel.pickupRequests.collectAsState()
    val pickupCounts by viewModel.pickupCounts.collectAsState()
    val campaignsByDate by viewModel.campaignsByDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
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
            onDateSelected = { date ->
                viewModel.selectDate(date)
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pickup requests and campaigns list
        if (selectedDate != null) {
            Column {
                PickupRequestsSection(
                    date = selectedDate!!,
                    requests = pickupRequests,
                    isLoading = isLoading
                )
                
                // Show campaigns for selected date - normalize date for lookup
                val normalizedDate = Calendar.getInstance().apply {
                    time = selectedDate!!
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                
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
        val hasCampaigns = dateCampaigns.isNotEmpty()
        
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> LojaSocialPrimary
                        hasCampaigns -> Color(0xFFFFE5B4) // Light orange/amber for campaign days
                        isToday -> LojaSocialPrimary.copy(alpha = 0.1f)
                        else -> LojaSocialSurface
                    }
                )
                .clickable { onDateSelected(date) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || isToday || hasCampaigns) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> LojaSocialOnPrimary
                        hasCampaigns -> Color(0xFFD97706) // Orange text for campaign days
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
    isLoading: Boolean
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
        } else if (requests.isEmpty()) {
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests) { request ->
                    PickupRequestItem(request = request)
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

