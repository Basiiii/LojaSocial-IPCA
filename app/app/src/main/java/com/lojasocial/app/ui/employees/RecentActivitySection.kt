package com.lojasocial.app.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark

@Composable
fun RecentActivitySection() {
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

@Preview(showBackground = true)
@Composable
fun RecentActivitySectionPreview() {
    RecentActivitySection()
}
