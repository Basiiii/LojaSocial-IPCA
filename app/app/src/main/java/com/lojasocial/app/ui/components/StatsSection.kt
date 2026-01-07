package com.lojasocial.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun StatsSection(
    pendingApplicationsCount: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            count = pendingApplicationsCount.toString(),
            label = "Pendentes",
            icon = Icons.Outlined.Schedule,
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFD97706)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            count = "4",
            label = "Agendados",
            icon = Icons.Outlined.CalendarToday,
            iconBg = Color(0xFFDBEAFE),
            iconTint = Color(0xFF2563EB)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatsSectionPreview() {
    StatsSection()
}
