package com.lojasocial.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StatsSection(
    pendingApplicationsCount: Int = 0,
    pendingRequestsCount: Int = 0,
    weeklyPickupsCount: Int = 0,
    onPendingRequestsClick: () -> Unit = {},
    onWeeklyPickupsClick: () -> Unit = {}
) {
    val pendingRequestsLabel = if (pendingRequestsCount == 1) "Pedido Pendente" else "Pedidos Pendentes"
    val weeklyPickupsLabel = if (weeklyPickupsCount == 1) "Agendamento da Semana" else "Agendamentos da Semana"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            count = pendingRequestsCount.toString(),
            label = pendingRequestsLabel,
            icon = Icons.Outlined.Schedule,
            iconBg = Color(0xFFFFF7ED), // Very Light Orange
            iconTint = Color(0xFFEA580C), // Orange
            onClick = onPendingRequestsClick
        )
        StatCard(
            modifier = Modifier.weight(1f),
            count = weeklyPickupsCount.toString(),
            label = weeklyPickupsLabel,
            icon = Icons.Outlined.CalendarToday,
            iconBg = Color(0xFFEFF6FF), // Very Light Blue
            iconTint = Color(0xFF2563EB), // Blue
            onClick = onWeeklyPickupsClick
        )
    }
}
@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
fun StatsSectionPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        StatsSection(
            pendingApplicationsCount = 12,
            pendingRequestsCount = 8,
            weeklyPickupsCount = 5
        )
    }
}