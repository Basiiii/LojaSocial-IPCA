package com.lojasocial.app.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.domain.application.ApplicationStatus
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.ui.calendar.CalendarView
import com.lojasocial.app.ui.chat.ChatView
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.components.StatsSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.profile.components.ProfileOption
import com.lojasocial.app.ui.stock.AddStockScreen
import com.lojasocial.app.ui.support.SupportView
// Import specific colors if they are not available globally, otherwise define locally
// import com.lojasocial.app.ui.theme.BrandBlue, etc.
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

// Define colors locally to ensure no missing reference errors
private val BrandBlue = Color(0xFF2563EB)
private val BrandOrange = Color(0xFFF97316)
private val BrandPurple = Color(0xFF7C3AED)

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
    onNavigateToMyApplications: () -> Unit = {}, // For viewing own applications
    onNavigateToExpiringItems: () -> Unit = {},
    onNavigateToActivityList: () -> Unit = {},
    onNavigateToCampaigns: () -> Unit = {},
    onNavigateToPickupRequests: () -> Unit = {},
    onNavigateToWeeklyPickups: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToBeneficiaries: () -> Unit = {},
    onNavigateToStockList: () -> Unit = {},
    onNavigateToUrgentRequest: () -> Unit = {},
    currentTab: String = "home",
    onTabChange: ((String) -> Unit)? = null
) {
    var showAddStockScreen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    var pendingRequestsCount by remember { mutableStateOf<Int?>(null) }
    var weeklyPickupsCount by remember { mutableStateOf(0) }
    val selectedTab = currentTab

    // Fetch User Profile for visibility rules (Admin checks)
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(userRepository) {
        userRepository.getCurrentUserProfile().collect { profile ->
            userProfile = profile
        }
    }

    // Get current user ID to exclude own applications from pending count
    val currentUserId = authRepository.getCurrentUser()?.uid
    var pendingApplicationsCount by remember { mutableStateOf(0) }

    // Fetch pending requests count and weekly pickups count
    LaunchedEffect(requestsRepository) {
        requestsRepository?.getAllRequests()?.collect { requests ->
            pendingRequestsCount = requests.count { it.status == 0 }

            val calendar = java.util.Calendar.getInstance()
            // Get start and end of current week
            calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startOfWeek = calendar.time

            calendar.add(java.util.Calendar.DAY_OF_WEEK, 6)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val endOfWeek = calendar.time

            weeklyPickupsCount = requests.count { request ->
                request.status == 1 &&
                        request.scheduledPickupDate != null &&
                        request.scheduledPickupDate!! >= startOfWeek &&
                        request.scheduledPickupDate!! <= endOfWeek
            }
        }
    }

    LaunchedEffect(applicationRepository, currentUserId) {
        applicationRepository?.getAllApplications()?.collect { applications ->
            pendingApplicationsCount = applications.count {
                it.status == ApplicationStatus.PENDING &&
                        it.userId != currentUserId
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
                        name = userName?.takeIf { it.isNotBlank() } ?: userProfile?.name ?: "Utilizador"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StatsSection(
                        pendingApplicationsCount = pendingApplicationsCount,
                        pendingRequestsCount = pendingRequestsCount ?: 0,
                        weeklyPickupsCount = weeklyPickupsCount,
                        onPendingRequestsClick = onNavigateToPickupRequests,
                        onWeeklyPickupsClick = onNavigateToWeeklyPickups
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Existing Buttons Section
                    QuickActionsSection(
                        onNavigateToScanStock = { showAddStockScreen = true },
                        onSupportClick = { onTabChange?.invoke("support") },
                        onNavigateToApplications = onNavigateToApplications,
                        onNavigateToPickupRequests = onNavigateToPickupRequests,
                        onNavigateToUrgentRequest = onNavigateToUrgentRequest,
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
                    onNavigateToApplications = onNavigateToMyApplications,
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
                    isBeneficiaryPortal = false,
                    profilePictureRepository = profilePictureRepository
                )
            }

            "gestao" -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Gestão e Ferramentas",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            // Admin/Employee Only Sections
                            if (userProfile?.isAdmin == true) {
                                ProfileOption(
                                    icon = Icons.Default.Warning,
                                    title = "Itens Próximos do Prazo",
                                    subtitle = "Ver itens a expirar em breve",
                                    iconColor = BrandOrange,
                                    onClick = onNavigateToExpiringItems
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))

                                ProfileOption(
                                    icon = Icons.Default.Campaign,
                                    title = "Campanhas",
                                    subtitle = "Gerir campanhas",
                                    iconColor = Color(0xFF06B6D4),
                                    onClick = onNavigateToCampaigns
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))

                                ProfileOption(
                                    icon = Icons.Default.History,
                                    title = "Registos de Auditoria",
                                    subtitle = "Ver registos de ações",
                                    iconColor = Color(0xFF6366F1),
                                    onClick = onNavigateToAuditLogs
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))

                                ProfileOption(
                                    icon = Icons.Default.People,
                                    title = "Beneficiários",
                                    subtitle = "Ver todos os beneficiários",
                                    iconColor = Color(0xFF10B981),
                                    onClick = onNavigateToBeneficiaries
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF8F8F8))

                                ProfileOption(
                                    icon = Icons.Default.Inventory2,
                                    title = "Stock",
                                    subtitle = "Ver produtos em stock",
                                    iconColor = Color(0xFF059669),
                                    onClick = onNavigateToStockList
                                )
                            } else {
                                // Non-admin employees see a message
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Apenas administradores têm acesso às ferramentas de gestão",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockScreen) {
        AddStockScreen(
            onNavigateBack = { showAddStockScreen = false }
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
                        // Fallback
                    }
                },
                subtitle = "Portal Funcionários",
                showTopBar = !(selectedTab == "support" && isChatOpen),
                showBottomBar = !(selectedTab == "support" && isChatOpen),
                showPortalSelection = showPortalSelection,
                onPortalSelectionClick = onPortalSelectionClick,
                onActivityClick = onNavigateToActivityList,
                isEmployee = true
            ) { paddingValues ->
                content(paddingValues)
            }
        } else {
            content(PaddingValues(0.dp))
        }
    }
}

// Local helper to match ProfileOption design without external dependency
@Composable
private fun MenuOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
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
            override suspend fun getAllBeneficiaries() = flow { emit(emptyList<UserProfile>()) }
            override suspend fun getAllUsers() = flow { emit(emptyList<UserProfile>()) }
        }

        val mockProfilePictureRepository = object : ProfilePictureRepository {
            override suspend fun uploadProfilePicture(uid: String, imageBase64: String) = Result.success(Unit)
            override suspend fun getProfilePicture(uid: String) = flow { emit(null) }
        }

        AppLayout(
            selectedTab = "home",
            onTabSelected = { },
            subtitle = "Portal Funcionários",
            isEmployee = true
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