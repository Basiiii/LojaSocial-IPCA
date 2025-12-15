package com.lojasocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Imports all outlined icons (Calendar, Help, etc.)
import androidx.compose.material.icons.outlined.* import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

// In your Components file (e.g., SharedComponents.kt)

// 1. Updated Top Bar (Accepts a subtitle)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(subtitle: String) { // Added subtitle parameter
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
                    Text(text = subtitle, fontSize = 12.sp, color = TextGray) // Dynamic subtitle
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
                        .size(8.dp).clip(CircleShape).background(Color.Red)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBgColor)
    )
}

// 2. Updated Greeting Section (Accepts name and message)
@Composable
fun GreetingSection(name: String, message: String) {
    Column {
        Text(text = "Olá, $name", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = message, fontSize = 14.sp, color = TextGray, lineHeight = 20.sp)
    }
}

// 3. Updated Action Card (Icon is now nullable)
// In the new design, the Green and Orange cards don't have the top-left icon box.
@Composable
fun ActionCard(
    title: String,
    description: String,
    buttonText: String,
    backgroundColor: Color,
    icon: ImageVector? = null, // Made nullable
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
            // Header: Only show row if there is an icon or a badge
            if (icon != null || badgeCount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Only show icon box if icon exists
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp)) // Filler if no icon but badge exists
                    }

                    if (badgeCount != null && badgeLabel != null) {
                        val badgeColor = if (isRedBadge) Color(0xFFEF4444) else Color(0xFFF59E0B)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(badgeColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "$badgeCount $badgeLabel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = backgroundColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}