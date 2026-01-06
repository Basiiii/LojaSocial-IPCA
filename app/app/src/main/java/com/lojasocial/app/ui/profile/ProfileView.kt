package com.lojasocial.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.profile.components.*
import com.lojasocial.app.ui.theme.AppBgColor
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.math.log

/**
 * Main profile view component.
 * 
 * This screen displays user profile information and provides access to
 * key app features like support, calendar, and logout functionality.
 * It uses a card-based layout with clean, modern design.
 * 
 * @param paddingValues Padding values for proper screen layout
 * @param authRepository Repository for authentication operations
 * @param userRepository Repository for user profile operations
 * @param onLogout Callback invoked when user logs out
 * @param onTabSelected Callback invoked for tab navigation
 */
@Composable
fun ProfileView(
    paddingValues: PaddingValues,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onLogout: () -> Unit,
    onTabSelected: (String) -> Unit,
    onNavigateToApplications: () -> Unit = {},
    onNavigateToExpiringItems: () -> Unit = {},
    onNavigateToCampaigns: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var showLogoutError by remember { mutableStateOf(false) }
    val userProfile = remember { mutableStateOf<UserProfile?>(null) }
    val currentUser = authRepository.getCurrentUser()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userRepository.getUserProfile(currentUser.uid).collect { profile ->
                userProfile.value = profile
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgColor)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        ProfileHeaderCard(userProfile.value)

        Spacer(modifier = Modifier.height(24.dp))

        QuickActionsCard(
            userProfile = userProfile.value,
            onSupportClick = { onTabSelected("support") },
            onCalendarClick = { onTabSelected("calendar") },
            onApplicationsClick = onNavigateToApplications,
            onExpiringItemsClick = onNavigateToExpiringItems,
            onCampaignsClick = onNavigateToCampaigns
        )

        Spacer(modifier = Modifier.height(40.dp))

        LogoutButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        authRepository.signOut()
                        onLogout()
                    } catch (e: Exception) {
                        showLogoutError = true
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppVersion()
    }

    
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

@Preview(showBackground = true)
@Composable
fun ProfileViewPreview() {
    val mockAuthRepository = object : AuthRepository {
        override suspend fun signIn(email: String, password: String) = TODO()
        override suspend fun signUp(email: String, password: String) = TODO()
        override suspend fun signOut() = TODO()
        override fun getCurrentUser() = TODO()
        override fun isUserLoggedIn() = TODO()
    }
    
    val mockUserRepository = object : UserRepository {
        override suspend fun getUserProfile(uid: String) = flow {
            emit(UserProfile(uid = "preview", email = "preview@lojasocial.pt", name = "Preview User", isAdmin = false, isBeneficiary = false))
        }
        override suspend fun getCurrentUserProfile() = flow {
            emit(UserProfile(uid = "preview", email = "preview@lojasocial.pt", name = "Preview User", isAdmin = false, isBeneficiary = false))
        }
        override suspend fun updateProfile(profile: UserProfile) = TODO()
        override suspend fun createProfile(profile: UserProfile) = TODO()
        override suspend fun saveFcmToken(token: String) = Result.success(Unit)
    }

    ProfileView(
        paddingValues = PaddingValues(0.dp),
        authRepository = mockAuthRepository,
        userRepository = mockUserRepository,
        onLogout = { },
        onTabSelected = { }
    )
}
