package com.lojasocial.app.ui.requestitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.RequestItem
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.InactiveFilterBackground
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.RedStock
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun ProductItemRow(
    requestItem: RequestItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    val stockColor = if (requestItem.stock > 5) BrandGreen else RedStock

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = requestItem.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = requestItem.category,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Disponível: ${requestItem.stock}",
                fontSize = 14.sp,
                color = stockColor,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantityButton(
                icon = Icons.Default.Remove,
                onClick = onRemove,
                enabled = quantity > 0,
                isRemoveButton = true
            )
            Text(
                text = "$quantity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            QuantityButton(
                icon = Icons.Default.Add,
                onClick = onAdd,
                enabled = enabled,
                isRemoveButton = false
            )
        }
    }
}

@Composable
private fun QuantityButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    isRemoveButton: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = LojaSocialPrimary,
            contentColor = Color.White,
            disabledContainerColor = if (isRemoveButton) LojaSocialPrimary else InactiveFilterBackground,
            disabledContentColor = if (isRemoveButton) Color.White else TextGray
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ProductItemRowPreview() {
    ProductItemRow(
        requestItem = RequestItem(id = "1", product = com.lojasocial.app.data.model.Product(name = "Arroz", category = 1)),
        quantity = 2,
        onAdd = { },
        onRemove = { },
        enabled = true
    )
}
