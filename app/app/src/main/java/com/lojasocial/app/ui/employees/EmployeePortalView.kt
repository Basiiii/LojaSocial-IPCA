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
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.request.RequestsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.domain.application.ApplicationStatus
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.components.StatsSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.stock.AddStockScreen
import com.lojasocial.app.ui.stock.DeleteStockScreen
import com.lojasocial.app.ui.support.SupportView
import com.lojasocial.app.ui.chat.ChatView
import com.lojasocial.app.ui.calendar.CalendarView
import kotlinx.coroutines.flow.flow

@Composable
fun EmployeePortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    profilePictureRepository: ProfilePictureRepository,
    expirationRepository: ExpirationRepository? = null,
    requestsRepository: RequestsRepository? = null,
    applicationRepository: ApplicationRepository? = null,
    onLogout: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {}, // For viewing all applications (employee portal home)
    onNavigateToMyApplications: () -> Unit = {}, // For viewing own applications (profile page)
    onNavigateToExpiringItems: () -> Unit = {},
    onNavigateToActivityList: () -> Unit = {},
    onNavigateToCampaigns: () -> Unit = {},
    onNavigateToPickupRequests: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToBeneficiaries: () -> Unit = {},
    onNavigateToStockList: () -> Unit = {},
    onNavigateToDeleteStock: () -> Unit = {}, // New callback for deleting stock
    currentTab: String = "home",
    onTabChange: ((String) -> Unit)? = null
) {
    var showAddStockScreen by remember { mutableStateOf(false) }
    var showDeleteStockScreen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    var pendingRequestsCount by remember { mutableStateOf<Int?>(null) }
    val selectedTab = currentTab
    
    // Get current user ID to exclude own applications
    val currentUserId = authRepository.getCurrentUser()?.uid
    
    // Fetch pending applications count (excluding current user's applications)
    var pendingApplicationsCount by remember { mutableStateOf(0) }
    
    // Fetch pending requests count
    LaunchedEffect(requestsRepository) {
        requestsRepository?.getAllRequests()?.collect { requests ->
            // Count requests with status 0 (SUBMETIDO)
            pendingRequestsCount = requests.count { it.status == 0 }
        }
    }
    
    LaunchedEffect(applicationRepository, currentUserId) {
        applicationRepository?.getAllApplications()?.collect { applications ->
            pendingApplicationsCount = applications.count { 
                it.status == ApplicationStatus.PENDING && 
                it.userId != currentUserId // Exclude current user's applications
            }
        }
    }

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
                    StatsSection(pendingApplicationsCount = pendingApplicationsCount)
                    Spacer(modifier = Modifier.height(24.dp))
                    QuickActionsSection(
                        onNavigateToScanStock = { showAddStockScreen = true },
                        onNavigateToDeleteStock = onNavigateToDeleteStock,
                        onSupportClick = { onTabChange?.invoke("support") },
                        onNavigateToApplications = onNavigateToApplications,
                        onNavigateToPickupRequests = onNavigateToPickupRequests,
                        pendingRequestsCount = pendingRequestsCount,
                        pendingApplicationsCount = pendingApplicationsCount
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    RecentActivitySection(
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
                    isBeneficiaryPortal = false,
                    onLogout = onLogout,
                    onTabSelected = { onTabChange?.invoke(it) },
                    onNavigateToApplications = onNavigateToMyApplications, // Use separate callback for own applications
                    onNavigateToExpiringItems = onNavigateToExpiringItems,
                    onNavigateToCampaigns = onNavigateToCampaigns,
                    onNavigateToAuditLogs = onNavigateToAuditLogs,
                    onNavigateToBeneficiaries = onNavigateToBeneficiaries,
                    onNavigateToStockList = onNavigateToStockList
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
                            showTopBar = false,
                            showBackButton = false,
                            onStartChat = { isChatOpen = true }
                        )
                    }
                }
            }

            "calendar" -> {
                CalendarView(
                    paddingValues = paddingValues,
                    isBeneficiaryPortal = false
                )
            }
        }
    }

    if (showAddStockScreen) {
        AddStockScreen(
            onNavigateBack = { showAddStockScreen = false }
        )
    } else if (showDeleteStockScreen) {
        DeleteStockScreen(
            onNavigateBack = { showDeleteStockScreen = false }
        )
    } else {
        if (useAppLayout) {
            AppLayout(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != "support") {
                        isChatOpen = false
                    }
                    onTabChange?.invoke(tab) ?: run {
                        // Fallback for preview/legacy usage
                    }
                },
                subtitle = "Portal Funcionários",
                showTopBar = !(selectedTab == "support" && isChatOpen),
                showBottomBar = !(selectedTab == "support" && isChatOpen),
                showPortalSelection = showPortalSelection,
                onPortalSelectionClick = onPortalSelectionClick,
                onActivityClick = onNavigateToActivityList
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
            override suspend fun saveFcmToken(token: String) = Result.success(Unit)
            override suspend fun getAllBeneficiaries() = flow {
                emit(emptyList<UserProfile>())
            }
        }
        
        val mockProfilePictureRepository = object : ProfilePictureRepository {
            override suspend fun uploadProfilePicture(uid: String, imageBase64: String) = Result.success(Unit)
            override suspend fun getProfilePicture(uid: String) = flow { emit(null) }
        }

        AppLayout(
            selectedTab = "home",
            onTabSelected = { },
            subtitle = "Portal Funcionários"
        ) {
            EmployeePortalView(
                useAppLayout = false,
                authRepository = mockAuthRepository,
                userRepository = mockUserRepository,
                profilePictureRepository = mockProfilePictureRepository
            )
        }
    }
}