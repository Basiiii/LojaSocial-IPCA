package com.lojasocial.app.domain.request

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lojasocial.app.ui.theme.BgBlue
import com.lojasocial.app.ui.theme.BgGreen
import com.lojasocial.app.ui.theme.BgRed
import com.lojasocial.app.ui.theme.BgYellow
import com.lojasocial.app.ui.theme.BrandOrange
import com.lojasocial.app.ui.theme.TextBlue
import com.lojasocial.app.ui.theme.TextGreen
import com.lojasocial.app.ui.theme.TextRed
import com.lojasocial.app.ui.theme.TextYellow


enum class RequestStatus(
    val label: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color
) {
    SUBMETIDO(
        label = "Pedido Submetido",
        icon = Icons.Default.Schedule,
        iconBgColor = BgYellow,
        iconTint = TextYellow
    ),

    PENDENTE_LEVANTAMENTO(
        label = "Pedido Aceite",
        icon = Icons.Default.Check,
        iconBgColor = BgGreen,
        iconTint = TextGreen
    ),

    CONCLUIDO(
        label = "Levantamento Concluído",
        icon = Icons.Default.ShoppingBag,
        iconBgColor = BgBlue,
        iconTint = TextBlue
    ),

    REJEITADO(
        label = "Pedido Rejeitado",
        icon = Icons.Default.Cancel,
        iconBgColor = BgRed,
        iconTint = TextRed
    ),

    CANCELADO(
    label = "Pedido Cancelado",
    icon = Icons.Default.Cancel,
    iconBgColor = BrandOrange,
    iconTint = TextRed
    )
}
