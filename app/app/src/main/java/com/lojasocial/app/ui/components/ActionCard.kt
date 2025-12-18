package com.lojasocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionCard(
    title: String,
    description: String,
    buttonText: String,
    backgroundColor: Color,
    icon: ImageVector?,
    badgeCount: Int? = null,
    badgeLabel: String? = null,
    isRedBadge: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Card Header: Icon + Badge
            if(icon != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }


                    // Optional Badge
                    if (badgeCount != null && badgeLabel != null) {
                        val badgeColor = if (isRedBadge) Color(0xFFEF4444) else Color(0xFFF59E0B)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(badgeColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$badgeCount $badgeLabel",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if(icon != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Card Text Content
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // THE ACTION BUTTON
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = backgroundColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionCardPreview() {
    ActionCard(
        title = "Adiciona ao Stock",
        description = "Adiciona um artigo ao stock, fazendo scan do código barras",
        buttonText = "Adiciona Item",
        backgroundColor = Color(0xFF10B981),
        icon = Icons.Default.QrCodeScanner,
        onClick = { }
    )
}
