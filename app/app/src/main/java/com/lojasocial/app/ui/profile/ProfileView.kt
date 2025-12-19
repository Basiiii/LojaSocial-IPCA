package com.lojasocial.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.theme.AppBgColor
import com.lojasocial.app.ui.theme.BrandBlue
import com.lojasocial.app.ui.theme.BrandGreen
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@Composable
fun ProfileView(paddingValues: PaddingValues, authRepository: AuthRepository, userRepository: UserRepository, onLogout: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var showLogoutError by remember { mutableStateOf(false) }
    
    // Get current user profile and check if user is logged in
    val userProfile: MutableState<UserProfile?> = remember { mutableStateOf<UserProfile?>(null) }
    val currentUser = authRepository.getCurrentUser()
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                userRepository.getUserProfile(currentUser.uid).collect { profile ->
                    userProfile.value = profile
                }
            } catch (e: Exception) {
                // Handle error - set to null or show error state
                userProfile.value = null
            }
        } else {
            userProfile.value = null
        }
    }
    
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(AppBgColor)
            .padding(16.dp)
    ) {
        // Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF064E3B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Profile Info
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    // Name
                    Text(
                        text = userProfile.value?.name ?: "Carregando...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Email
                    Text(
                        text = userProfile.value?.email ?: "carregando@lojasocial.pt",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Role
                    Text(
                        text = when {
                            userProfile.value?.isAdmin == true -> "Administrador"
                            userProfile.value?.isBeneficiary == true -> "Beneficiário"
                            else -> "Funcionária"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Review Applications Button
        Button(
            onClick = { /* TODO: Navigate to applications screen */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "Applications",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rever Candidaturas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Logout Button
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        authRepository.signOut()
                        // Navigation will be handled automatically by auth state listener
                    } catch (e: Exception) {
                        showLogoutError = true
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terminar Sessão",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // App Version
        Text(
            text = "Versão 1.0.0",
            fontSize = 12.sp,
            color = TextGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
    
    // Logout Error Alert
    if (showLogoutError) {
        AlertDialog(
            onDismissRequest = { showLogoutError = false },
            title = {
                Text("Erro ao Terminar Sessão")
            },
            text = {
                Text("Ocorreu um erro ao tentar terminar a sessão. Por favor, tente novamente.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutError = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextGray
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Arrow",
            tint = TextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileViewPreview() {
    // Mock auth repository for preview
    val mockAuthRepository = object : AuthRepository {
        override suspend fun signIn(email: String, password: String) = TODO()
        override suspend fun signUp(email: String, password: String) = TODO()
        override suspend fun signOut() = TODO()
        override fun getCurrentUser() = TODO()
        override fun isUserLoggedIn() = TODO()
    }
    
    // Mock user repository for preview
    val mockUserRepository = object : UserRepository {
        override suspend fun getUserProfile(uid: String) = kotlinx.coroutines.flow.flow { 
            emit(UserProfile(uid = "preview", email = "preview@lojasocial.pt", name = "Preview User", isAdmin = false, isBeneficiary = false))
        }
        override suspend fun getCurrentUserProfile() = kotlinx.coroutines.flow.flow { 
            emit(UserProfile(uid = "preview", email = "preview@lojasocial.pt", name = "Preview User", isAdmin = false, isBeneficiary = false))
        }
        override suspend fun updateProfile(profile: UserProfile) = TODO()
        override suspend fun createProfile(profile: UserProfile) = TODO()
    }
    
    ProfileView(
        paddingValues = PaddingValues(0.dp),
        authRepository = mockAuthRepository,
        userRepository = mockUserRepository,
        onLogout = { }
    )
}
