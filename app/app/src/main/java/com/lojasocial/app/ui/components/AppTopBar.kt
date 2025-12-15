package com.lojasocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(subtitle: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF064E3B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "LojaSocial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text(text = subtitle, fontSize = 12.sp, color = TextGray)
                }
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificações", tint = TextDark)
                }
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp)
                        .size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Red)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBgColor)
    )
}
