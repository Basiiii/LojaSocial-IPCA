package com.lojasocial.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lojasocial.app.ui.theme.BrandGreen

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = selectedTab == "home",
            onClick = { onTabSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandGreen,
                selectedTextColor = BrandGreen,
                indicatorColor = Color(0xFFDCFCE7)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Suporte") },
            label = { Text("Suporte") },
            selected = selectedTab == "support",
            onClick = { onTabSelected("support") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandGreen,
                selectedTextColor = BrandGreen,
                indicatorColor = Color(0xFFDCFCE7)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Calendário") },
            label = { Text("Calendário") },
            selected = selectedTab == "calendar",
            onClick = { onTabSelected("calendar") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandGreen,
                selectedTextColor = BrandGreen,
                indicatorColor = Color(0xFFDCFCE7)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") },
            selected = selectedTab == "profile",
            onClick = { onTabSelected("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandGreen,
                selectedTextColor = BrandGreen,
                indicatorColor = Color(0xFFDCFCE7)
            )
        )
    }
}
