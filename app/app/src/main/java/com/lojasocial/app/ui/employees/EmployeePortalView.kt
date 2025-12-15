package com.lojasocial.app.ui.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray


// --- Main Screen ---
@Composable
fun EmployeePortalView() {
    Scaffold(
        topBar = { EmployeeTopBar() },
        bottomBar = { EmployeeBottomBar() },
        containerColor = AppBgColor
    ) { paddingValues ->
        // Main Content Scrollable Area
        Column(
            modifier = Modifier
                .padding(paddingValues) // Respects Top/Bottom bar padding
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            GreetingSection()
            Spacer(modifier = Modifier.height(16.dp))
            StatsSection()
            Spacer(modifier = Modifier.height(24.dp))
            QuickActionsSection()
            Spacer(modifier = Modifier.height(24.dp))
            RecentActivitySection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Component: Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF064E3B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "LojaSocial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Portal Funcionários",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        },
        actions = {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notificações",
                        tint = TextDark
                    )
                }
                // Notification Dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppBgColor
        )
    )
}

// --- Component: Bottom Bar ---
@Composable
fun EmployeeBottomBar() {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandGreen,
                selectedTextColor = BrandGreen,
                indicatorColor = Color(0xFFDCFCE7)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.HelpOutline, contentDescription = "Suporte") },
            label = { Text("Suporte") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Calendário") },
            label = { Text("Calendário") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") },
            selected = false,
            onClick = { }
        )
    }
}

// --- Component: Greeting ---
@Composable
fun GreetingSection() {
    Column {
        Text(
            text = "Olá, Mónica",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Acompanha candidaturas, controla pedidos e gere o stock facilmente",
            fontSize = 14.sp,
            color = TextGray,
            lineHeight = 20.sp
        )
    }
}

// --- Component: Stats Row ---
@Composable
fun StatsSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            count = "4",
            label = "Pendentes",
            icon = Icons.Outlined.Schedule,
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFD97706)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            count = "4",
            label = "Agendados",
            icon = Icons.Outlined.CalendarToday,
            iconBg = Color(0xFFDBEAFE),
            iconTint = Color(0xFF2563EB)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    count: String,
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = count,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Text(text = label, fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

// --- Component: Quick Actions (The Colorful Cards) ---
@Composable
fun QuickActionsSection() {
    Column {
        Text(
            text = "Ações Rápidas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Adiciona ao Stock",
            description = "Adiciona um artigo ao stock, fazendo scan do código barras",
            buttonText = "Adiciona Item",
            backgroundColor = BrandGreen,
            icon = Icons.Default.QrCodeScanner,
            onClick = { /* Handle Add Item */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Pedidos Levantamento",
            description = "Gere pedidos de levantamento de bens",
            buttonText = "Gere Pedidos",
            backgroundColor = BrandPurple,
            icon = Icons.Default.Inventory2,
            badgeCount = 12,
            badgeLabel = "Pendentes",
            onClick = { /* Handle Orders */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            title = "Ver Candidaturas",
            description = "Revê e processa candidaturas pendentes",
            buttonText = "Rever Candidaturas",
            backgroundColor = BrandBlue,
            icon = Icons.Default.Description,
            badgeCount = 5,
            badgeLabel = "Novas",
            isRedBadge = true,
            onClick = { /* Handle Applications */ }
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    buttonText: String,
    backgroundColor: Color,
    icon: ImageVector,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Translucent Icon Box
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

            Spacer(modifier = Modifier.height(16.dp))

            // Card Text Content
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // THE ACTION BUTTON
            // This replaces the generic box with a functional Material Button
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

// --- Component: Recent Activity ---
@Composable
fun RecentActivitySection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Atividade Recente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Ver tudo",
                fontSize = 14.sp,
                color = BrandBlue,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActivityItem(
            title = "Levantamento Concluído",
            subtitle = "José Alves - Alimentar",
            time = "há 15 minutos",
            icon = Icons.Default.Inventory,
            iconBg = Color(0xFFDBEAFE),
            iconTint = BrandBlue
        )
        ActivityItem(
            title = "Candidatura Aceite",
            subtitle = "Marco Cardoso",
            time = "há 27 minutos",
            icon = Icons.Default.Check,
            iconBg = Color(0xFFDCFCE7),
            iconTint = BrandGreen
        )
        ActivityItem(
            title = "Nova Candidatura",
            subtitle = "Enrique Rodrigues",
            time = "há 1 hora",
            icon = Icons.Default.Description,
            iconBg = Color(0xFFF3E8FF),
            iconTint = BrandPurple
        )
    }
}

@Composable
fun ActivityItem(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark
                )
                Text(text = subtitle, fontSize = 12.sp, color = TextGray)
                Text(text = time, fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true, heightDp = 1000)
@Composable
fun EmployeeScreenPreview() {
    MaterialTheme {
        EmployeePortalView()
    }
}