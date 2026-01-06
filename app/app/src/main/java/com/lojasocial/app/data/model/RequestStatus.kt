package com.lojasocial.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lojasocial.app.ui.theme.BgBlue
import com.lojasocial.app.ui.theme.BgRed
import com.lojasocial.app.ui.theme.BgYellow
import com.lojasocial.app.ui.theme.TextBlue
import com.lojasocial.app.ui.theme.TextRed
import com.lojasocial.app.ui.theme.TextYellow


enum class RequestStatus(
    val label: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color
) {
    PENDENTE(
        label = "Levantamento Pendente",
        icon = Icons.Default.Schedule, // Clock icon
        iconBgColor = BgYellow,
        iconTint = TextYellow
    ),
    CONCLUIDO(
        label = "Levantamento Concluído",
        icon = Icons.Default.ShoppingBag, // Bag/Box icon
        iconBgColor = BgBlue,
        iconTint = TextBlue
    ),
    REJEITADO(
        label = "Pedido Rejeitado",
        icon = Icons.Default.Cancel, // X icon
        iconBgColor = BgRed,
        iconTint = TextRed
    )
}