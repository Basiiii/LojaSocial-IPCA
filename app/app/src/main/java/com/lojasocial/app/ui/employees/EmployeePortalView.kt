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
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.BottomNavigationBar
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.components.StatCard
import com.lojasocial.app.ui.components.ActionCard
import com.lojasocial.app.ui.components.ActivityItem
import com.lojasocial.app.ui.components.StatsSection
import com.lojasocial.app.ui.employees.RecentActivitySection
import com.lojasocial.app.ui.employees.QuickActionsSection
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.BrandPurple
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray

@Composable
fun EmployeePortalView(paddingValues: PaddingValues) {
    // Main Content Scrollable Area
    Column(
        modifier = Modifier
            .padding(paddingValues)
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

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun EmployeeScreenPreview() {
    MaterialTheme {
        EmployeePortalView(PaddingValues(0.dp))
    }
}