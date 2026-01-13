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

import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.support.SupportView
import com.lojasocial.app.ui.chat.ChatView
import com.lojasocial.app.ui.calendar.CalendarView
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect

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
    profilePictureRepository: ProfilePictureRepository,
    expirationRepository: ExpirationRepository? = null,
    requestsRepository: RequestsRepository? = null,
    onLogout: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    onNavigateToExpiringItems: () -> Unit = {},
    onNavigateToActivityList: () -> Unit = {},
    currentTab: String = "home",
    onTabChange: ((String) -> Unit)? = null
) {
    var isChatOpen by remember { mutableStateOf(false) }
    var pendingRequestsCount by remember { mutableStateOf<Int?>(null) }
    
    // Fetch pending requests count for current user
    LaunchedEffect(requestsRepository) {
        requestsRepository?.getRequests()?.collect { requests ->
            // Count requests with status 0 (SUBMETIDO) - only current user's requests
            pendingRequestsCount = requests.count { it.status == 0 }
        }
    }

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
                        onSupportClick = { onTabChange?.invoke("support") },
                        pendingRequestsCount = pendingRequestsCount
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
                    profilePictureRepository = profilePictureRepository,
                    isBeneficiaryPortal = true,
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
                CalendarView(
                    paddingValues = paddingValues,
                    isBeneficiaryPortal = true,
                    profilePictureRepository = profilePictureRepository
                )
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
            onPortalSelectionClick = onPortalSelectionClick,
            onActivityClick = onNavigateToActivityList,
            isEmployee = false
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
            override suspend fun getAllBeneficiaries() = flow {
                emit(emptyList<UserProfile>())
            }
            override suspend fun getAllUsers() = flow {
                emit(emptyList<UserProfile>())
            }
        }
        
        val mockProfilePictureRepository = object : ProfilePictureRepository {
            override suspend fun uploadProfilePicture(uid: String, imageBase64: String) = Result.success(Unit)
            override suspend fun getProfilePicture(uid: String) = flow { emit(null) }
        }

        BeneficiaryPortalView(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository,
            profilePictureRepository = mockProfilePictureRepository,
            requestsRepository = null,
            onNavigateToOrders = {},
            onNavigateToPickups = {}
        )
    }
}