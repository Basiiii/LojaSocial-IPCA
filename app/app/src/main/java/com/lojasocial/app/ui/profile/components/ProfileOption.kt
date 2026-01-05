package com.lojasocial.app.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

/**
 * Profile option component for navigation items.
 * 
 * This component displays a clickable option with an icon, title, and subtitle.
 * It's designed to be used within cards for profile navigation options.
 * 
 * @param icon The icon to display for this option
 * @param title The main title text for the option
 * @param subtitle The descriptive subtitle text
 * @param enabled Whether this option is clickable (default: true)
 * @param iconColor The color for the icon and background (default: BrandBlue)
 * @param onClick Callback invoked when the option is clicked
 */
@Composable
fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    iconColor: Color = BrandBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (enabled) iconColor.copy(alpha = 0.1f) else Color(0xFFF3F4F6),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) iconColor else Color.LightGray,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) TextDark else Color.LightGray
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextGray
            )
        }

        if (enabled) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
