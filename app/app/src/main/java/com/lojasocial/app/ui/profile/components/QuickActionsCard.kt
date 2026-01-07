package com.lojasocial.app.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandOrange
import com.lojasocial.app.ui.theme.BrandPurple

/**
 * Quick actions card containing support, calendar, applications, and expiring items options.
 * 
 * This component displays the main navigation options from the profile screen
 * in a single card with dividers between options. It provides quick access
 * to support, calendar, applications functionality (for beneficiaries only), and
 * expiring items (for admins/employees only).
 * 
 * Visibility rules:
 * - Support and Calendar: Always visible
 * - Applications ("As minhas Candidaturas"): Visible for all users (beneficiaries, non-beneficiaries, and employees)
 * - Expiring Items ("Itens Próximos do Prazo"): Only visible for admins/employees
 * 
 * @param userProfile The user profile data to determine which options should be shown
 * @param onSupportClick Callback invoked when support option is clicked
 * @param onCalendarClick Callback invoked when calendar option is clicked
 * @param onApplicationsClick Callback invoked when applications option is clicked
 * @param onExpiringItemsClick Callback invoked when expiring items option is clicked (admins only)
 * @param onCampaignsClick Callback invoked when campaigns option is clicked (admins only)
 */
@Composable
fun QuickActionsCard(
    userProfile: UserProfile?,
    onSupportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onApplicationsClick: () -> Unit = {},
    onExpiringItemsClick: () -> Unit = {},
    onCampaignsClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ProfileOption(
                icon = Icons.Default.SupportAgent,
                title = "Suporte",
                subtitle = "FAQs e chat de ajuda",
                iconColor = BrandBlue,
                onClick = onSupportClick
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))
            
            ProfileOption(
                icon = Icons.Default.DateRange,
                title = "Calendário",
                subtitle = "Ver eventos e atividades",
                iconColor = BrandPurple,
                onClick = onCalendarClick
            )
            
            // Show applications option for all users (beneficiaries, non-beneficiaries, and employees)
            // Everyone should be able to see and manage their own applications
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))
            
            ProfileOption(
                icon = Icons.Default.Assignment,
                title = "As minhas Candidaturas",
                subtitle = "Ver e gerir as minhas candidaturas",
                iconColor = Color(0xFF10B981),
                onClick = onApplicationsClick
            )
            
            // Show expiring items option only for admins/employees
            if (userProfile?.isAdmin == true) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))
                
                ProfileOption(
                    icon = Icons.Default.Warning,
                    title = "Itens Próximos do Prazo",
                    subtitle = "Ver itens a expirar em breve",
                    iconColor = BrandOrange,
                    onClick = onExpiringItemsClick
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))
                
                ProfileOption(
                    icon = Icons.Default.Campaign,
                    title = "Campanhas",
                    subtitle = "Gerir campanhas",
                    iconColor = Color(0xFF9333EA),
                    onClick = onCampaignsClick
                )
            }
        }
    }
}
