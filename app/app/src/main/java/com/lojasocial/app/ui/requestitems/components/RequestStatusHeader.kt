package com.lojasocial.app.ui.requestitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.LightBlue
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.ProgressFull

@Composable
fun RequestStatusHeader(
    totalItemsSelected: Int,
    maxItems: Int,
    progress: Float,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$totalItemsSelected de $maxItems artigos selecionados",
                color = LojaSocialPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Limpar",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(onClick = onClearClick)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = ProgressFull,
            trackColor = ProgressFull,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RequestStatusHeaderPreview() {
    MaterialTheme {
        RequestStatusHeader(
            totalItemsSelected = 3,
            maxItems = 10,
            progress = 0.3f,
            onClearClick = {}
        )
    }
}