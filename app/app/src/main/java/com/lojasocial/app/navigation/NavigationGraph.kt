package com.lojasocial.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.ui.applications.ApplicationDetailView
import com.lojasocial.app.ui.applications.ApplicationsListView
import com.lojasocial.app.ui.beneficiaries.BeneficiaryPortalView
import com.lojasocial.app.ui.employees.EmployeePortalView
import com.lojasocial.app.ui.login.LoginScreen
import com.lojasocial.app.ui.nonbeneficiaries.NonBeneficiaryPortalView
import com.lojasocial.app.ui.portalselection.PortalSelectionView
import com.lojasocial.app.ui.requestitems.RequestItemsView
import com.lojasocial.app.ui.submitApplications.CandidaturaAcademicDataView
import com.lojasocial.app.ui.submitApplications.CandidaturaDocumentsView
import com.lojasocial.app.ui.submitApplications.CandidaturaPersonalInfoView
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.lojasocial.app.ui.campaigns.CreateCampaignScreen

/**
 * Builds the complete navigation graph for the application.
 * 
 * @param navController The navigation controller
 * @param startDestination The initial route to navigate to
 * @param lastProfile The last loaded user profile (for passing to portal views)
 * @param navigationError Error message to display, if any
 * @param onNavigationErrorChange Callback when navigation error changes
 * @param onProfileChange Callback when profile changes
 * @param authRepository Authentication repository
 * @param userRepository User repository
 * @param applicationRepository Application repository
 * @param expirationRepository Expiration repository for checking expiring items
 */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    startDestination: String,
    lastProfile: UserProfile?,
    navigationError: String?,
    onNavigationErrorChange: (String?) -> Unit,
    onProfileChange: (UserProfile?) -> Unit,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    applicationRepository: ApplicationRepository,
    expirationRepository: ExpirationRepository? = null,
    campaignRepository: CampaignRepository? = null,
    requestsRepository: RequestsRepository? = null,
    profilePictureRepository: ProfilePictureRepository
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                externalError = navigationError,
                onLoginSuccess = {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            onNavigationErrorChange(null)
                            val profile = userRepository.getCurrentUserProfile().first()
                            if (profile != null) {
                                onProfileChange(profile)
                                val destination = NavigationHelper.getDestinationForUser(profile)
                                if (destination == Screen.Login.route) {
                                    onNavigationErrorChange("Perfil inválido.")
                                } else {
                                    navController.navigate(destination) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            } else {
                                onNavigationErrorChange("Erro ao carregar perfil.")
                            }
                        } catch (_: Exception) {
                            onNavigationErrorChange("Erro de conexão.")
                        }
                    }
                }
            )
        }

        // Portal Selection
        composable(Screen.PortalSelection.route) {
            val displayName = lastProfile?.name?.substringBefore(" ") ?: "Utilizador"
            PortalSelectionView(
                userName = displayName,
                onNavigateToEmployeePortal = { 
                    navController.navigate(Screen.EmployeePortal.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onNavigateToBeneficiaryPortal = { 
                    navController.navigate(Screen.BeneficiaryPortal.route) {
                        popUpTo(Screen.PortalSelection.route) { inclusive = true }
                    }
                },
                onLogout = {
                    CoroutineScope(Dispatchers.Main).launch {
                        authRepository.signOut()
                        navController.navigate(Screen.Login.route) { 
                            popUpTo(0) { inclusive = true } 
                        }
                    }
                }
            )
        }

        // Employee Portal with nested tab navigation
        navigation(
            startDestination = Screen.EmployeePortal.Home.route,
            route = Screen.EmployeePortal.route
        ) {
            composable(Screen.EmployeePortal.Home.route) { backStackEntry ->
                EmployeePortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
                    requestsRepository = requestsRepository,
                    applicationRepository = applicationRepository
                )
            }
            composable(Screen.EmployeePortal.Profile.route) { backStackEntry ->
                EmployeePortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
                    requestsRepository = requestsRepository,
                    applicationRepository = applicationRepository
                )
            }
            composable(Screen.EmployeePortal.Support.route) { backStackEntry ->
                EmployeePortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
                    requestsRepository = requestsRepository,
                    applicationRepository = applicationRepository
                )
            }
            composable(Screen.EmployeePortal.Calendar.route) { backStackEntry ->
                EmployeePortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
                    requestsRepository = requestsRepository,
                    applicationRepository = applicationRepository
                )
            }
        }

        // Beneficiary Portal with nested tab navigation
        navigation(
            startDestination = Screen.BeneficiaryPortal.Home.route,
            route = Screen.BeneficiaryPortal.route
        ) {
            composable(Screen.BeneficiaryPortal.Home.route) { backStackEntry ->
                BeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository
                )
            }
            composable(Screen.BeneficiaryPortal.Profile.route) { backStackEntry ->
                BeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository
                )
            }
            composable(Screen.BeneficiaryPortal.Support.route) { backStackEntry ->
                BeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository
                )
            }
            composable(Screen.BeneficiaryPortal.Calendar.route) { backStackEntry ->
                BeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository,
                    expirationRepository = expirationRepository
                )
            }
        }

        // Non-Beneficiary Portal with nested tab navigation
        navigation(
            startDestination = Screen.NonBeneficiaryPortal.Home.route,
            route = Screen.NonBeneficiaryPortal.route
        ) {
            composable(Screen.NonBeneficiaryPortal.Home.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Profile.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Support.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Calendar.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    profilePictureRepository = profilePictureRepository
                )
            }
        }

        // Request Items
        composable(Screen.RequestItems.route) {
            RequestItemsView(
                onBackClick = { navController.navigateUp() },
                onSubmitClick = {
                    navController.navigate(Screen.BeneficiaryPortal.route) {
                        popUpTo(Screen.BeneficiaryPortal.route) { inclusive = false }
                    }
                }
            )
        }

        // Application Flow
        navigation(
            startDestination = Screen.ApplicationFlow.PersonalInfo.route,
            route = Screen.ApplicationFlow.route
        ) {
            composable(Screen.ApplicationFlow.PersonalInfo.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ApplicationFlow.route)
                }
                val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                CandidaturaPersonalInfoView(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateNext = { navController.navigate(Screen.ApplicationFlow.AcademicData.route) },
                    viewModel = viewModel
                )
            }

            composable(Screen.ApplicationFlow.AcademicData.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ApplicationFlow.route)
                }
                val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                CandidaturaAcademicDataView(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateNext = { navController.navigate(Screen.ApplicationFlow.Documents.route) },
                    viewModel = viewModel
                )
            }

            composable(Screen.ApplicationFlow.Documents.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ApplicationFlow.route)
                }
                val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                CandidaturaDocumentsView(
                    onNavigateBack = { navController.navigateUp() },
                    onSubmit = {
                        navController.popBackStack(Screen.ApplicationFlow.route, inclusive = true)
                    },
                    viewModel = viewModel
                )
            }
        }

        // Applications List (for users - shows only their applications)
        composable(Screen.ApplicationsList.route) {
            ApplicationsListView(
                applicationRepository = applicationRepository,
                onNavigateBack = { navController.navigateUp() },
                onAddClick = {
                    navController.navigate(Screen.ApplicationFlow.route)
                },
                onItemClick = { applicationId ->
                    navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                },
                showAllApplications = false,
                isBeneficiary = lastProfile?.isBeneficiary == true, // Hide add button if user is already a beneficiary
                title = "As minhas Candidaturas" // Custom title for profile view
            )
        }

        // All Applications List (for employees - shows all applications except their own)
        composable(Screen.AllApplicationsList.route) {
            val currentUserId = authRepository.getCurrentUser()?.uid
            
            ApplicationsListView(
                applicationRepository = applicationRepository,
                onNavigateBack = { navController.navigateUp() },
                onAddClick = {}, // No add button for employees
                onItemClick = { applicationId ->
                    navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                },
                showAllApplications = true,
                excludeCurrentUserId = currentUserId // Exclude employee's own applications
            )
        }

        // Application Detail
        composable(Screen.ApplicationDetail().route) { backStackEntry ->
            val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
            // Check if we came from AllApplicationsList (employee view)
            val isEmployeeView = navController.previousBackStackEntry?.destination?.route == Screen.AllApplicationsList.route
            ApplicationDetailView(
                applicationId = applicationId,
                applicationRepository = applicationRepository,
                onNavigateBack = { navController.navigateUp() },
                isEmployeeView = isEmployeeView
            )
        }

        // Expiring Items
        composable(Screen.ExpiringItems.route) {
            com.lojasocial.app.ui.expiringitems.ExpiringItemsView(
                onNavigateBack = { 
                    // Try to pop back if there's a back stack entry
                    // If not (e.g., from notification), navigate to appropriate portal home
                    if (!navController.popBackStack()) {
                        // Determine which portal to navigate to based on user profile
                        val destination = when {
                            lastProfile?.isAdmin == true && lastProfile.isBeneficiary -> Screen.BeneficiaryPortal.Home.route
                            lastProfile?.isAdmin == true -> Screen.EmployeePortal.Home.route
                            else -> Screen.BeneficiaryPortal.Home.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.ExpiringItems.route) { inclusive = true }
                        }
                    }
                },
                expirationRepository = expirationRepository
            )
        }

        // Activity List
        composable(Screen.ActivityList.route) { backStackEntry ->
            // Determine if user is employee based on:
            // 1. If coming from employee portal, show employee activities
            // 2. Otherwise, check if user is admin (regardless of beneficiary status)
            val isFromEmployeePortal = navController.previousBackStackEntry?.destination?.route?.startsWith(Screen.EmployeePortal.route) == true
            val isFromBeneficiaryPortal = navController.previousBackStackEntry?.destination?.route?.startsWith(Screen.BeneficiaryPortal.route) == true
            
            val isEmployee = when {
                isFromEmployeePortal -> true // Coming from employee portal, show employee activities
                isFromBeneficiaryPortal -> false // Coming from beneficiary portal, show beneficiary activities
                else -> lastProfile?.isAdmin == true // Default: if admin, show employee activities
            }
            
            com.lojasocial.app.ui.activity.ActivityListView(
                isEmployee = isEmployee,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Audit Logs
        composable(Screen.AuditLogs.route) {
            com.lojasocial.app.ui.audit.AuditLogsView(
                onNavigateBack = {
                    // Try to pop back if there's a back stack entry
                    // If not (e.g., from notification), navigate to employee portal profile
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.EmployeePortal.Profile.route) {
                            popUpTo(Screen.AuditLogs.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Pickup Requests
        composable(Screen.PickupRequests.route) {
            // Check if we're coming from beneficiary portal by checking previous destination
            val isFromBeneficiaryPortal = navController.previousBackStackEntry?.destination?.route?.startsWith(Screen.BeneficiaryPortal.route) == true
            com.lojasocial.app.ui.requests.PickupRequestsView(
                onNavigateBack = { navController.navigateUp() },
                userRepository = userRepository,
                requestsRepository = requestsRepository,
                profilePictureRepository = profilePictureRepository,
                filterByCurrentUser = isFromBeneficiaryPortal
            )
        }

        // Campaigns List
        composable(Screen.CampaignsList.route) {
            campaignRepository?.let { repository ->
                com.lojasocial.app.ui.campaigns.CampaignsListView(
                    campaignRepository = repository,
                    onNavigateBack = { navController.navigateUp() },
                    onAddClick = {
                        navController.navigate(Screen.CreateCampaign.route)
                    },
                    onEditClick = { campaign ->
                        // Navigate to edit campaign screen
                        navController.navigate(Screen.CreateCampaign.Edit.createRoute(campaign.id))
                    },
                    onCampaignClick = { campaign ->
                        // Navigate to campaign products view
                        navController.navigate(Screen.CampaignProducts.createRoute(campaign.id))
                    }
                )
            } ?: run {
                // Show error if repository is not available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Erro ao carregar campanhas")
                }
            }
        }

        // Create Campaign
        composable(Screen.CreateCampaign.route) {
            CreateCampaignScreen(
                campaignRepository = campaignRepository,
                campaignToEdit = null,
                onNavigateBack = { navController.navigateUp() },
                onCampaignSaved = {
                    navController.popBackStack()
                }
            )
        }
        
        // Edit Campaign
        composable(Screen.CreateCampaign.Edit().route) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            CreateCampaignScreen(
                campaignRepository = campaignRepository,
                campaignId = campaignId,
                onNavigateBack = { navController.navigateUp() },
                onCampaignSaved = {
                    navController.popBackStack()
                }
            )
        }
        
        // Campaign Products
        composable(Screen.CampaignProducts().route) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            campaignRepository?.let { repository ->
                // Fetch campaign to pass to view
                var campaign by remember { mutableStateOf<com.lojasocial.app.domain.campaign.Campaign?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                
                LaunchedEffect(campaignId) {
                    campaign = repository.getCampaignById(campaignId)
                    isLoading = false
                }
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    campaign != null -> {
                        com.lojasocial.app.ui.campaigns.CampaignProductsView(
                            campaign = campaign!!,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Campanha não encontrada")
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Erro ao carregar campanha")
                }
            }
        }
    }
}

/**
 * Helper composable for Employee Portal tab content.
 * This extracts the portal view logic to work with navigation-based tabs.
 */
@Composable
private fun EmployeePortalTabContent(
    tab: String,
    profile: UserProfile?,
    navController: NavHostController,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    profilePictureRepository: ProfilePictureRepository,
    expirationRepository: ExpirationRepository? = null,
    campaignRepository: CampaignRepository? = null,
    requestsRepository: RequestsRepository? = null,
    applicationRepository: ApplicationRepository? = null
) {
    // Observe current user to get fresh profile data - this ensures the name updates when user changes
    val currentUser = authRepository.getCurrentUser()
    var currentProfile by remember(currentUser?.uid) { mutableStateOf<UserProfile?>(profile) }
    
    // Update profile when current user changes
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            try {
                userRepository.getCurrentUserProfile()
                    .catch { e ->
                        // Handle Firestore errors gracefully (e.g., permission denied after logout)
                        if (e is FirebaseFirestoreException || e is Exception) {
                            currentProfile = null
                        }
                    }
                    .collect { newProfile ->
                        // Only update if we still have a valid user
                        if (authRepository.getCurrentUser() != null && newProfile != null && newProfile.uid == currentUser.uid) {
                            currentProfile = newProfile
                        } else {
                            currentProfile = null
                        }
                    }
            } catch (e: Exception) {
                currentProfile = null
            }
        } else {
            currentProfile = null
        }
    }
    
    // Also update when profile parameter changes (from navigation) - this handles initial load
    LaunchedEffect(profile?.uid) {
        if (profile != null && currentUser?.uid == profile.uid) {
            currentProfile = profile
        }
    }
    
    val showPortalSelection = currentProfile?.isAdmin == true && currentProfile?.isBeneficiary == true
    val displayName = currentProfile?.name?.substringBefore(" ") ?: "Utilizador"

    EmployeePortalView(
        userName = displayName,
        showPortalSelection = showPortalSelection,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        authRepository = authRepository,
        userRepository = userRepository,
        profilePictureRepository = profilePictureRepository,
        expirationRepository = expirationRepository,
        requestsRepository = requestsRepository,
        applicationRepository = applicationRepository,
        onLogout = {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onNavigateToApplications = {
            // For "Ver Candidaturas" button in employee portal home - show all applications except employee's own
            navController.navigate(Screen.AllApplicationsList.route)
        },
        onNavigateToMyApplications = {
            // For "As minhas Candidaturas" in profile - show only user's own applications
            navController.navigate(Screen.ApplicationsList.route)
        },
        onNavigateToExpiringItems = {
            navController.navigate(Screen.ExpiringItems.route)
        },
        onNavigateToActivityList = {
            navController.navigate(Screen.ActivityList.route)
        },
        onNavigateToCampaigns = {
            navController.navigate(Screen.CampaignsList.route)
        },
        onNavigateToPickupRequests = {
            navController.navigate(Screen.PickupRequests.route)
        },
        onNavigateToAuditLogs = {
            navController.navigate(Screen.AuditLogs.route)
        },
        currentTab = tab,
        onTabChange = { newTab ->
            val route = when (newTab) {
                "home" -> Screen.EmployeePortal.Home.route
                "profile" -> Screen.EmployeePortal.Profile.route
                "support" -> Screen.EmployeePortal.Support.route
                "calendar" -> Screen.EmployeePortal.Calendar.route
                else -> Screen.EmployeePortal.Home.route
            }
            navController.navigate(route) {
                popUpTo(Screen.EmployeePortal.route) { inclusive = false }
            }
        }
    )
}

/**
 * Helper composable for Beneficiary Portal tab content.
 */
@Composable
private fun BeneficiaryPortalTabContent(
    tab: String,
    profile: UserProfile?,
    navController: NavHostController,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    profilePictureRepository: ProfilePictureRepository,
    expirationRepository: ExpirationRepository? = null
) {
    // Observe current user to get fresh profile data - this ensures the name updates when user changes
    val currentUser = authRepository.getCurrentUser()
    var currentProfile by remember(currentUser?.uid) { mutableStateOf<UserProfile?>(profile) }
    
    // Update profile when current user changes
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            try {
                userRepository.getCurrentUserProfile()
                    .catch { e ->
                        // Handle Firestore errors gracefully (e.g., permission denied after logout)
                        if (e is FirebaseFirestoreException || e is Exception) {
                            currentProfile = null
                        }
                    }
                    .collect { newProfile ->
                        // Only update if we still have a valid user
                        if (authRepository.getCurrentUser() != null && newProfile != null && newProfile.uid == currentUser.uid) {
                            currentProfile = newProfile
                        } else {
                            currentProfile = null
                        }
                    }
            } catch (e: Exception) {
                currentProfile = null
            }
        } else {
            currentProfile = null
        }
    }
    
    // Also update when profile parameter changes (from navigation) - this handles initial load
    LaunchedEffect(profile?.uid) {
        if (profile != null && currentUser?.uid == profile.uid) {
            currentProfile = profile
        }
    }
    
    val showPortalSelection = currentProfile?.isAdmin == true && currentProfile?.isBeneficiary == true
    val displayName = currentProfile?.name?.substringBefore(" ") ?: "Utilizador"
    
    BeneficiaryPortalView(
        userName = displayName,
        showPortalSelection = showPortalSelection,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        onNavigateToOrders = { navController.navigate(Screen.RequestItems.route) },
        onNavigateToPickups = { navController.navigate(Screen.PickupRequests.route) },
        authRepository = authRepository,
        userRepository = userRepository,
        profilePictureRepository = profilePictureRepository,
        expirationRepository = expirationRepository,
        onLogout = {
            navController.navigate(Screen.Login.route) { 
                popUpTo(0) { inclusive = true } 
            }
        },
        onNavigateToApplications = {
            navController.navigate(Screen.ApplicationsList.route)
        },
        onNavigateToExpiringItems = {
            navController.navigate(Screen.ExpiringItems.route)
        },
        onNavigateToActivityList = {
            navController.navigate(Screen.ActivityList.route)
        },
        currentTab = tab,
        onTabChange = { newTab ->
            val route = when (newTab) {
                "home" -> Screen.BeneficiaryPortal.Home.route
                "profile" -> Screen.BeneficiaryPortal.Profile.route
                "support" -> Screen.BeneficiaryPortal.Support.route
                "calendar" -> Screen.BeneficiaryPortal.Calendar.route
                else -> Screen.BeneficiaryPortal.Home.route
            }
            navController.navigate(route) {
                popUpTo(Screen.BeneficiaryPortal.route) { inclusive = false }
            }
        }
    )
}

/**
 * Helper composable for Non-Beneficiary Portal tab content.
 */
@Composable
private fun NonBeneficiaryPortalTabContent(
    tab: String,
    profile: UserProfile?,
    navController: NavHostController,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    profilePictureRepository: ProfilePictureRepository
) {
    // Observe current user to get fresh profile data - this ensures the name updates when user changes
    val currentUser = authRepository.getCurrentUser()
    var currentProfile by remember(currentUser?.uid) { mutableStateOf<UserProfile?>(profile) }
    
    // Update profile when current user changes
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            try {
                userRepository.getCurrentUserProfile()
                    .catch { e ->
                        // Handle Firestore errors gracefully (e.g., permission denied after logout)
                        if (e is FirebaseFirestoreException || e is Exception) {
                            currentProfile = null
                        }
                    }
                    .collect { newProfile ->
                        // Only update if we still have a valid user
                        if (authRepository.getCurrentUser() != null && newProfile != null && newProfile.uid == currentUser.uid) {
                            currentProfile = newProfile
                        } else {
                            currentProfile = null
                        }
                    }
            } catch (e: Exception) {
                currentProfile = null
            }
        } else {
            currentProfile = null
        }
    }
    
    // Also update when profile parameter changes (from navigation) - this handles initial load
    LaunchedEffect(profile?.uid) {
        if (profile != null && currentUser?.uid == profile.uid) {
            currentProfile = profile
        }
    }
    
    val displayName = currentProfile?.name?.substringBefore(" ") ?: "Utilizador"
    
    NonBeneficiaryPortalView(
        userName = displayName,
        showPortalSelection = false,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        authRepository = authRepository,
        userRepository = userRepository,
        profilePictureRepository = profilePictureRepository,
        onNavigateToApplication = {
            navController.navigate(Screen.ApplicationFlow.route)
        },
        onLogout = {
            navController.navigate(Screen.Login.route) { 
                popUpTo(0) { inclusive = true } 
            }
        },
        onNavigateToApplications = {
            navController.navigate(Screen.ApplicationsList.route)
        },
        currentTab = tab,
        onTabChange = { newTab ->
            val route = when (newTab) {
                "home" -> Screen.NonBeneficiaryPortal.Home.route
                "profile" -> Screen.NonBeneficiaryPortal.Profile.route
                "support" -> Screen.NonBeneficiaryPortal.Support.route
                "calendar" -> Screen.NonBeneficiaryPortal.Calendar.route
                else -> Screen.NonBeneficiaryPortal.Home.route
            }
            navController.navigate(route) {
                popUpTo(Screen.NonBeneficiaryPortal.route) { inclusive = false }
            }
        }
    )
}
