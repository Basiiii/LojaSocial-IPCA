package com.lojasocial.app.ui.requestitems.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.DisabledBtn
import com.lojasocial.app.ui.theme.LojaSocialPrimary
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun RequestItemsBottomBar(
    onSubmitClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Button(
            onClick = onSubmitClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LojaSocialPrimary,
                contentColor = Color.White,
                disabledContainerColor = DisabledBtn,
                disabledContentColor = TextGray
            )
        ) {
            Text("Submeter Pedido", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RequestItemsBottomBarPreview() {
    RequestItemsBottomBar(
        onSubmitClick = {},
        enabled = true
    )
}

@Preview(showBackground = true)
@Composable
fun RequestItemsBottomBarDisabledPreview() {
    RequestItemsBottomBar(
        onSubmitClick = {},
        enabled = false
    )
}