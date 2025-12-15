package com.lojasocial.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun GreetingSection(
    name: String = "Mónica",
    message: String = "Acompanha candidaturas, controla pedidos e gere o stock facilmente"
) {
    androidx.compose.foundation.layout.Column {
        Text(
            text = "Olá, $name",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = TextGray,
            lineHeight = 20.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingSectionPreview() {
    GreetingSection()
}
