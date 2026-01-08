package com.lojasocial.app.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lojasocial.app.repository.user.UserProfile

/**
 * Profile header card component.
 * 
 * This component wraps the ProfileHeader in a styled card with proper
 * elevation and rounded corners. It provides the container for the
 * user profile information at the top of the profile screen.
 * 
 * @param profile The user profile data to display
 * @param onEditPictureClick Callback invoked when the edit icon is clicked
 */
@Composable
fun ProfileHeaderCard(
    profile: UserProfile?,
    onEditPictureClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProfileHeader(
            profile = profile,
            modifier = Modifier.padding(20.dp),
            onEditPictureClick = onEditPictureClick
        )
    }
}
