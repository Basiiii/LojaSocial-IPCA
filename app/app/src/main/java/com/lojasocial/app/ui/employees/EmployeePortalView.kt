package com.lojasocial.app.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.components.StatsSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.stock.AddStockScreen
import com.lojasocial.app.ui.support.SupportView
import com.lojasocial.app.ui.chat.ChatView
import kotlinx.coroutines.flow.flow

@Composable
fun EmployeePortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onLogout: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    var showAddStockScreen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }

    val content = @Composable { paddingValues: PaddingValues ->
        when (selectedTab) {
            "home" -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    GreetingSection(
                        name = userName?.takeIf { it.isNotBlank() } ?: "Utilizador"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StatsSection()
                    Spacer(modifier = Modifier.height(24.dp))
                    QuickActionsSection(
                        onNavigateToScanStock = { showAddStockScreen = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    RecentActivitySection()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            "profile" -> {
                ProfileView(
                    paddingValues = paddingValues,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    onLogout = onLogout,
                    onTabSelected = { selectedTab = it },
                    onNavigateToApplications = onNavigateToApplications
                )
            }

            "support" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isChatOpen) {
                                Modifier
                            } else {
                                Modifier.padding(paddingValues)
                            }
                        )
                ) {
                    if (isChatOpen) {
                        ChatView(
                            embeddedInAppLayout = true,
                            onClose = { isChatOpen = false }
                        )
                    } else {
                        SupportView(
                            showTopBar = false,
                            showBackButton = false,
                            onStartChat = { isChatOpen = true }
                        )
                    }
                }
            }

            "calendar" -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Calendário (por implementar)")
                }
            }
        }
    }

    if (showAddStockScreen) {
        AddStockScreen(
            onNavigateBack = {}
        )
    } else {
        if (useAppLayout) {
            AppLayout(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                subtitle = "Portal Funcionários",
                showPortalSelection = showPortalSelection,
                onPortalSelectionClick = onPortalSelectionClick
            ) { paddingValues ->
                content(paddingValues)
            }
        } else {
            content(PaddingValues(0.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun EmployeeScreenPreview() {
    MaterialTheme {
        val mockAuthRepository = object : AuthRepository {
            override suspend fun signIn(email: String, password: String) = TODO()
            override suspend fun signUp(email: String, password: String) = TODO()
            override suspend fun signOut() = TODO()
            override fun getCurrentUser() = TODO()
            override fun isUserLoggedIn() = TODO()
        }

        val mockUserRepository = object : UserRepository {
            override suspend fun getUserProfile(uid: String) = flow {
                emit(
                    UserProfile(
                        uid = "preview",
                        email = "preview@lojasocial.pt",
                        name = "Preview User",
                        isAdmin = true,
                        isBeneficiary = false
                    )
                )
            }

            override suspend fun getCurrentUserProfile() = flow {
                emit(
                    UserProfile(
                        uid = "preview",
                        email = "preview@lojasocial.pt",
                        name = "Preview User",
                        isAdmin = true,
                        isBeneficiary = false
                    )
                )
            }

            override suspend fun updateProfile(profile: UserProfile) = TODO()
            override suspend fun createProfile(profile: UserProfile) = TODO()
        }

        AppLayout(
            selectedTab = "home",
            onTabSelected = { },
            subtitle = "Portal Funcionários"
        ) {
            EmployeePortalView(
                useAppLayout = false,
                authRepository = mockAuthRepository,
                userRepository = mockUserRepository
            )
        }
    }
}