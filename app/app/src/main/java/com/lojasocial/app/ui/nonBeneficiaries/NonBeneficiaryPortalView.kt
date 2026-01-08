package com.lojasocial.app.ui.nonbeneficiaries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.support.SupportView
import com.lojasocial.app.ui.chat.ChatView
import com.lojasocial.app.ui.calendar.CalendarView
import com.lojasocial.app.ui.theme.CardBlue
import kotlinx.coroutines.flow.flow

/**
 * Main portal view for non-beneficiary users of the Loja Social application.
 * 
 * This composable provides the main interface for users who are not beneficiaries
 * of the social support program. It offers access to scholarship applications,
 * profile management, and portal selection functionality. The view uses a tab-based
 * navigation system and can be displayed with or without the app layout wrapper.
 * 
 * Features:
 * - Tab-based navigation (Home, Support, Calendar, Profile)
 * - Scholarship application access
 * - Profile management integration
 * - Full support functionality with chat integration
 * - Portal selection for user type switching
 * - Responsive design with scrollable content
 * - Portuguese user interface
 * - Optional app layout wrapper
 * 
 * @param useAppLayout Whether to use the AppLayout wrapper (default: true)
 * @param userName Optional user name for personalized greeting
 * @param showPortalSelection Whether to show portal selection options
 * @param onPortalSelectionClick Callback for portal selection interaction
 * @param authRepository Repository for authentication operations
 * @param userRepository Repository for user profile operations
 * @param onCandidaturaClick Callback for scholarship application navigation
 * @param onNavigateToApplication Callback for application form navigation
 * @param onLogout Callback for logout navigation
 */
@Composable
fun NonBeneficiaryPortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    profilePictureRepository: ProfilePictureRepository,
    onCandidaturaClick: () -> Unit = {},
    onNavigateToApplication: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    currentTab: String = "home",
    onTabChange: ((String) -> Unit)? = null
) {
    /**
     * Current selected tab state for navigation.
     * 
     * Possible values:
     * - "home": Home tab with main options
     * - "profile": User profile tab
     * - "support": Support tab with chat functionality
     * - "calendar": Calendar tab (placeholder)
     */
    var isChatOpen by remember { mutableStateOf(false) }
    val selectedTab = currentTab

    /**
     * Main content composable based on selected tab.
     * 
     * This composable renders different content based on the currently
     * selected tab, providing a clean separation of concerns for each
     * section of the portal.
     */
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
                        name = userName?.takeIf { it.isNotBlank() } ?: "Utilizador",
                        message = "Realiza a tua candidatura para aceder aos benefícios disponíveis"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionsSection(onCandidaturaClick = onNavigateToApplication)

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
                    onNavigateToApplications = onNavigateToApplications,
                    onNavigateToExpiringItems = {},
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
                    isBeneficiaryPortal = false
                )
            }
        }
    }

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
            subtitle = "Portal Candidatos",
            showTopBar = !(selectedTab == "support" && isChatOpen),
            showBottomBar = !(selectedTab == "support" && isChatOpen),
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

/**
 * Quick actions section for the non-beneficiary portal.
 * 
 * This composable provides a section containing quick action cards
 * for common user tasks, currently focused on scholarship application access.
 * It serves as a container for action cards that provide direct access
 * to important features.
 * 
 * @param onCandidaturaClick Callback invoked when the scholarship application card is clicked
 */
@Composable
fun QuickActionsSection(
    onCandidaturaClick: () -> Unit
) {
    CandidaturaCard(onClick = onCandidaturaClick)
}

/**
 * Scholarship application card component.
 * 
 * This composable displays a prominent card for scholarship application
 * access. It features a blue gradient background, descriptive text,
 * and a call-to-action button. The card is designed to be visually
 * appealing and encourage user engagement with the application process.
 * 
 * Design features:
 * - Rounded corners with large radius (24dp)
 * - Blue gradient background (CardBlue)
 * - White icon with semi-transparent background
 * - Hierarchical text layout
 * - White button with blue text
 * - Forward arrow icon for navigation indication
 * 
 * @param onClick Callback invoked when the card or button is clicked
 */
@Composable
fun CandidaturaCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            /**
             * Icon container with semi-transparent white background.
             */
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            /**
             * Main title for the scholarship application.
             */
            Text(
                text = "Realizar Candidatura",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            /**
             * Descriptive text explaining the purpose.
             */
            Text(
                text = "Realiza a tua candidatura para aceder aos benefícios disponíveis",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            /**
             * Call-to-action button with navigation arrow.
             */
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = CardBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Realizar Candidatura",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Preview composable for the NonBeneficiaryPortalView.
 * 
 * This preview demonstrates the non-beneficiary portal with mock repositories
 * for development and design purposes. It shows the complete interface
 * with the scholarship application card and navigation structure.
 * 
 * The preview uses mock implementations of AuthRepository and UserRepository
 * to provide realistic data without requiring actual authentication.
 */
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun NonBeneficiaryPreview() {
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
                        isAdmin = false,
                        isBeneficiary = false
                    )
                )
            }

            override suspend fun updateProfile(profile: UserProfile) = TODO()
            override suspend fun createProfile(profile: UserProfile) = TODO()
            override suspend fun saveFcmToken(token: String) = Result.success(Unit)
        }
        
        val mockProfilePictureRepository = object : ProfilePictureRepository {
            override suspend fun uploadProfilePicture(uid: String, imageBase64: String) = Result.success(Unit)
            override suspend fun getProfilePicture(uid: String) = flow { emit(null) }
        }

        NonBeneficiaryPortalView(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository,
            profilePictureRepository = mockProfilePictureRepository
        )
    }
}