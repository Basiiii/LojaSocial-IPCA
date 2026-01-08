package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lojasocial.app.domain.activity.Activity
import com.lojasocial.app.domain.activity.ActivityType
import com.lojasocial.app.ui.activity.ActivityViewModel
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandOrange
import com.lojasocial.app.ui.theme.TextDark
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
private fun getActivityIconAndColors(type: ActivityType): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    return when (type) {
        ActivityType.REQUEST_SUBMITTED -> Triple(
            Icons.Default.Schedule,
            Color(0xFFFEF3C7),
            BrandOrange
        )
        ActivityType.REQUEST_ACCEPTED -> Triple(
            Icons.Default.Check,
            Color(0xFFDCFCE7),
            BrandGreen
        )
        ActivityType.PICKUP_COMPLETED -> Triple(
            Icons.Default.ShoppingBag,
            Color(0xFFDBEAFE),
            BrandBlue
        )
        else -> Triple(
            Icons.Default.Schedule,
            Color(0xFFFEF3C7),
            BrandOrange
        )
    }
}

@Composable
fun ActivitySection(
    viewModel: ActivityViewModel = hiltViewModel(),
    onViewAllClick: () -> Unit = {}
) {
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActivitiesForBeneficiary(limit = 20)
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
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && activities.isEmpty()) {
            // Show loading state or empty state
            Text(
                text = "A carregar...",
                fontSize = 14.sp,
                color = com.lojasocial.app.ui.theme.TextGray
            )
        } else if (activities.isEmpty()) {
            Text(
                text = "Sem atividades recentes",
                fontSize = 14.sp,
                color = com.lojasocial.app.ui.theme.TextGray
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
fun ActivitySectionPreview() {
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
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            title = "Levantamento Concluído",
            subtitle = "Levantamento feito com sucesso",
            time = "há 2 minutos",
            icon = Icons.Default.ShoppingBag,
            iconBg = Color(0xFFDBEAFE),
            iconTint = BrandBlue
        )

        ActivityItem(
            title = "Pedido Aceite",
            subtitle = "O teu pedido foi aceite",
            time = "há 15 minutos",
            icon = Icons.Default.Check,
            iconBg = Color(0xFFDCFCE7),
            iconTint = BrandGreen
        )
    }
}
