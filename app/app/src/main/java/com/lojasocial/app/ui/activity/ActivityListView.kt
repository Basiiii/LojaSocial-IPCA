package com.lojasocial.app.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.activity.Activity
import com.lojasocial.app.domain.activity.ActivityType
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.theme.*
import java.util.concurrent.TimeUnit

/**
 * Helper function to format time ago in Portuguese format ("há X minutos/horas")
 */
private fun formatTimeAgo(date: java.util.Date): String {
    val now = java.util.Date()
    val diffInMillis = now.time - date.time
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
    
    return when {
        minutes < 1 -> "Agora"
        minutes < 60 -> "há ${minutes}min"
        hours < 24 -> "há ${hours}h"
        days < 7 -> "há ${days}d"
        else -> "há ${days}d"
    }
}

/**
 * Helper function to get icon, background color, and tint color for activity type
 * These match the icons and colors used in RecentActivitySection
 */
private fun getActivityIconAndColors(type: ActivityType): Triple<ImageVector, Color, Color> {
    return when (type) {
        ActivityType.REQUEST_SUBMITTED -> Triple(
            Icons.Default.Inventory,
            Color(0xFFDBEAFE),
            BrandBlue
        )
        ActivityType.REQUEST_ACCEPTED -> Triple(
            Icons.Default.Check,
            Color(0xFFDCFCE7),
            BrandGreen
        )
        ActivityType.PICKUP_COMPLETED -> Triple(
            Icons.Default.Inventory,
            Color(0xFFDBEAFE),
            BrandBlue
        )
        ActivityType.APPLICATION_SUBMITTED -> Triple(
            Icons.Default.Description,
            Color(0xFFF3E8FF),
            BrandPurple
        )
        ActivityType.APPLICATION_APPROVED -> Triple(
            Icons.Default.Check,
            Color(0xFFDCFCE7),
            BrandGreen
        )
        ActivityType.REQUEST_REJECTED -> Triple(
            Icons.Default.Cancel,
            Color(0xFFFEE2E2),
            Color(0xFFEF4444)
        )
        ActivityType.APPLICATION_REJECTED -> Triple(
            Icons.Default.Cancel,
            Color(0xFFFEE2E2),
            Color(0xFFEF4444)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListView(
    isEmployee: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(isEmployee) {
        // Load a large number of activities to show all of them
        if (isEmployee) {
            viewModel.loadActivitiesForEmployee(limit = 100)
        } else {
            viewModel.loadActivitiesForBeneficiary(limit = 100)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Atividade Recente",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBgColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Erro ao carregar atividades",
                                color = TextDark,
                                fontSize = 16.sp
                            )
                            Text(
                                text = error ?: "Erro desconhecido",
                                color = TextGray,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    if (isEmployee) {
                                        viewModel.loadActivitiesForEmployee(limit = 100)
                                    } else {
                                        viewModel.loadActivitiesForBeneficiary(limit = 100)
                                    }
                                }
                            ) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                }
                activities.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sem atividades recentes",
                            color = TextGray,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(
                            items = activities,
                            key = { it.id }
                        ) { activity: Activity ->
                            val (icon, iconBg, iconTint) = getActivityIconAndColors(activity.type)
                            ActivityItem(
                                title = activity.title,
                                subtitle = activity.subtitle,
                                time = formatTimeAgo(activity.timestamp),
                                icon = icon,
                                iconBg = iconBg,
                                iconTint = iconTint
                            )
                        }
                    }
                }
            }
        }
    }
}
