package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.ExpirationRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.support.SupportView
import com.lojasocial.app.ui.chat.ChatView
import com.lojasocial.app.ui.calendar.CalendarView
import kotlinx.coroutines.flow.flow

@Composable
fun BeneficiaryPortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    onNavigateToOrders: (() -> Unit)? = null,
    onNavigateToPickups: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    expirationRepository: ExpirationRepository? = null,
    onLogout: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    onNavigateToExpiringItems: () -> Unit = {},
    onNavigateToActivityList: () -> Unit = {},
    currentTab: String = "home",
    onTabChange: ((String) -> Unit)? = null
) {
    var isChatOpen by remember { mutableStateOf(false) }

    val content = @Composable { paddingValues: PaddingValues ->
        when (currentTab) {
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
                        name = userName?.takeIf { it.isNotBlank() } ?: "Beneficiário",
                        message = "Recebe apoio quando precisares e acompanha os teus pedidos"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionsSection(
                        onNavigateToOrders = onNavigateToOrders ?: {},
                        onNavigateToPickups = onNavigateToPickups ?: {},
                        onSupportClick = { onTabChange?.invoke("support") }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ActivitySection(
                        onViewAllClick = onNavigateToActivityList
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            "profile" -> {
                ProfileView(
                    paddingValues = paddingValues,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    onLogout = onLogout,
                    onTabSelected = { onTabChange?.invoke(it) },
                    onNavigateToApplications = onNavigateToApplications,
                    onNavigateToExpiringItems = onNavigateToExpiringItems,
                    onNavigateToCampaigns = { /* Will be passed from navigation */ }
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
                            embeddedInAppLayout = false,
                            onClose = { isChatOpen = false }
                        )
                    } else {
                        SupportView(
                            onStartChat = { isChatOpen = true },
                            showTopBar = false
                        )
                    }
                }
            }

            "calendar" -> {
                CalendarView(paddingValues = paddingValues)
            }
        }
    }

    if (useAppLayout) {
        AppLayout(
            selectedTab = currentTab,
            onTabSelected = { tab ->
                if (tab != "support") {
                    isChatOpen = false
                }
                onTabChange?.invoke(tab) ?: run {
                    // Fallback for preview/legacy usage
                }
            },
            subtitle = "Portal Beneficiários",
            showTopBar = !(currentTab == "support" && isChatOpen),
            showBottomBar = !(currentTab == "support" && isChatOpen),
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun BeneficiaryPreview() {
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
                        isAdmin = false,
                        isBeneficiary = true
                    )
                )
            }

            override suspend fun getCurrentUserProfile() = flow {
                emit(
                    UserProfile(
                        uid = "preview",
                        email = "preview@lojasocial.pt",
                        name = "Preview User",
                        isAdmin = false,
                        isBeneficiary = true
                    )
                )
            }

            override suspend fun updateProfile(profile: UserProfile) = TODO()
            override suspend fun createProfile(profile: UserProfile) = TODO()
            override suspend fun saveFcmToken(token: String) = Result.success(Unit)
        }

        BeneficiaryPortalView(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository,
            onNavigateToOrders = {},
            onNavigateToPickups = {}
        )
    }
}