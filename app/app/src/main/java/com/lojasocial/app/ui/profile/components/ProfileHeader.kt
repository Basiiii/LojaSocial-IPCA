package com.lojasocial.app.ui.profile.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import com.lojasocial.app.utils.FileUtils

/**
 * Profile header component displaying user information.
 * 
 * This component shows the user's profile picture, name, and email
 * in a clean, card-based layout. It's designed to be used at the
 * top of the profile screen.
 * 
 * @param profile The user profile data containing name and email
 * @param modifier Optional modifier for custom styling
 * @param onEditPictureClick Callback invoked when the edit icon is clicked
 */
@Composable
fun ProfileHeader(
    profile: UserProfile?,
    modifier: Modifier = Modifier,
    onEditPictureClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
        ) {
            // Profile picture or placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onEditPictureClick),
                contentAlignment = Alignment.Center
            ) {
                val profilePicture = profile?.profilePicture
                if (!profilePicture.isNullOrBlank()) {
                    // Decode Base64 to ImageBitmap
                    val imageBitmap = remember(profilePicture) {
                        try {
                            val bytes = FileUtils.convertBase64ToFile(profilePicture).getOrNull()
                            bytes?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        // Fallback to icon if decoding fails
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // Placeholder icon
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextDark,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            // Edit icon overlay at bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onEditPictureClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit profile picture",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = profile?.name ?: "Carregando...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = profile?.email ?: "",
                fontSize = 14.sp,
                color = TextGray
            )
        }
    }
}
