package com.lojasocial.app.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.data.model.Activity
import com.lojasocial.app.data.model.ActivityType
import com.lojasocial.app.ui.activity.ActivityViewModel
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
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
    }
}

@Composable
fun RecentActivitySection(
    viewModel: ActivityViewModel = hiltViewModel(),
    onViewAllClick: () -> Unit = {}
) {
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActivitiesForEmployee(limit = 20)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Atividade Recente",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && activities.isEmpty()) {
            // Show loading state
            Text(
                text = "A carregar...",
                fontSize = 14.sp,
                color = TextGray
            )
        } else if (activities.isEmpty()) {
            Text(
                text = "Sem atividades recentes",
                fontSize = 14.sp,
                color = TextGray
            )
        } else {
            for (activity in activities.take(3)) {
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

@Preview(showBackground = true)
@Composable
fun RecentActivitySectionPreview() {
    // Preview with mock data (ViewModel not available in previews)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Atividade Recente",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            title = "Levantamento Concluído",
            subtitle = "José Alves - Alimentar",
            time = "há 15 minutos",
            icon = Icons.Default.Inventory,
            iconBg = Color(0xFFDBEAFE),
            iconTint = BrandBlue
        )
        ActivityItem(
            title = "Candidatura Aceite",
            subtitle = "Marco Cardoso",
            time = "há 27 minutos",
            icon = Icons.Default.Check,
            iconBg = Color(0xFFDCFCE7),
            iconTint = BrandGreen
        )
        ActivityItem(
            title = "Nova Candidatura",
            subtitle = "Enrique Rodrigues",
            time = "há 1 hora",
            icon = Icons.Default.Description,
            iconBg = Color(0xFFF3E8FF),
            iconTint = BrandPurple
        )
    }
}
