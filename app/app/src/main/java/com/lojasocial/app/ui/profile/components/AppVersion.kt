package com.lojasocial.app.ui.profile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.TextGray

/**
 * App version display component.
 * 
 * This component displays the current version of the application
 * at the bottom of the profile screen with subtle styling.
 * 
 * @param version The version string to display (default: "1.0.0")
 */
@Composable
fun AppVersion(version: String = "1.0.0") {
    Text(
        text = "Versão $version",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        color = TextGray.copy(alpha = 0.6f)
    )
}
