package com.lojasocial.app.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.application.ApplicationStatus
import com.lojasocial.app.ui.theme.ButtonGreen
import com.lojasocial.app.ui.theme.TextDarkGray
import com.lojasocial.app.ui.theme.DisabledBtn

/**
 * Status tab selector specifically for filtering applications by status.
 * 
 * @param selectedStatus The currently selected status filter (null means "Todos")
 * @param onStatusSelected Callback when a status is selected
 */
@Composable
fun ApplicationStatusTab(
    selectedStatus: ApplicationStatus?,
    onStatusSelected: (ApplicationStatus?) -> Unit
) {
    val statusOptions = listOf(
        null to "Todos",
        ApplicationStatus.PENDING to "Pendentes",
        ApplicationStatus.APPROVED to "Aceites",
        ApplicationStatus.REJECTED to "Rejeitadas"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        statusOptions.forEach { (status, label) ->
            val isSelected = status == selectedStatus

            // Determine styles based on state
            val backgroundColor = if (isSelected) ButtonGreen else DisabledBtn
            val textColor = if (isSelected) Color.White else TextDarkGray
            val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

            Box(
                modifier = Modifier
                    .clip(CircleShape) // Makes it pill-shaped
                    .background(backgroundColor)
                    .clickable { onStatusSelected(status) }
                    .padding(horizontal = 24.dp, vertical = 10.dp), // Inner padding
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun ApplicationStatusTabPreview() {
    ApplicationStatusTab(
        selectedStatus = ApplicationStatus.REJECTED,
        onStatusSelected = {}
    )
}

