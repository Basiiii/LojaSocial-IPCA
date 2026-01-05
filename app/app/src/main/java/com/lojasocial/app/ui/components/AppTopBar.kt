package com.lojasocial.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.outlined.Apps
import com.lojasocial.app.R
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    subtitle: String,
    notifications: Boolean = true,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF064E3B))
                        .let { if (showPortalSelection && onPortalSelectionClick != null) it.clickable { onPortalSelectionClick() } else it },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.loja_social_small),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = "LojaSocial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark, lineHeight = 16.sp)
                    Text(text = subtitle, fontSize = 12.sp, color = TextGray, lineHeight = 12.sp)
                }
            }
        },
        actions = {
            if (notifications) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificações", tint = TextDark)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Red)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBgColor)
    )
}

@Preview(showBackground = true)
@Composable
fun AppTopBarPreview() {
    AppTopBar(subtitle = "Bem-vindo de volta!")
}
