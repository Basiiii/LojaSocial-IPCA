package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.theme.BrandOrange

@Composable
fun ActivitySection() {
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
                color = com.lojasocial.app.ui.theme.TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = com.lojasocial.app.ui.theme.BrandBlue,
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
            iconTint = com.lojasocial.app.ui.theme.BrandBlue
        )

        ActivityItem(
            title = "Pedido Aceite",
            subtitle = "O teu pedido foi aceite",
            time = "há 15 minutos",
            icon = Icons.Default.Check,
            iconBg = Color(0xFFDCFCE7),
            iconTint = com.lojasocial.app.ui.theme.BrandGreen
        )

        ActivityItem(
            title = "Pedido Submetido",
            subtitle = "Pedido submetido e pendente",
            time = "há 1 hora",
            icon = Icons.Default.Schedule,
            iconBg = Color(0xFFFEF3C7),
            iconTint = BrandOrange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActivitySectionPreview() {
    ActivitySection()
}
