package com.lojasocial.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.lojasocial.app.repository.ApplicationRepository
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.CampaignRepository
import com.lojasocial.app.repository.ExpirationRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.repository.UserRepository
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
import kotlinx.coroutines.launch
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
    campaignRepository: CampaignRepository? = null
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
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
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
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
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
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
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
                    expirationRepository = expirationRepository,
                    campaignRepository = campaignRepository,
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
                    userRepository = userRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Profile.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Support.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository
                )
            }
            composable(Screen.NonBeneficiaryPortal.Calendar.route) { backStackEntry ->
                NonBeneficiaryPortalTabContent(
                    tab = NavigationHelper.getTabFromRoute(backStackEntry.destination.route ?: ""),
                    profile = lastProfile,
                    navController = navController,
                    authRepository = authRepository,
                    userRepository = userRepository
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
        composable(Screen.ActivityList.route) {
            // Determine if user is employee based on current profile
            val isEmployee = lastProfile?.isAdmin == true && !lastProfile.isBeneficiary
            
            com.lojasocial.app.ui.activity.ActivityListView(
                isEmployee = isEmployee,
                onNavigateBack = { navController.navigateUp() }
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
    expirationRepository: ExpirationRepository? = null,
    campaignRepository: CampaignRepository? = null,
    applicationRepository: ApplicationRepository? = null
) {
    val showPortalSelection = profile?.isAdmin == true && profile.isBeneficiary
    val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"

    EmployeePortalView(
        userName = displayName,
        showPortalSelection = showPortalSelection,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        authRepository = authRepository,
        userRepository = userRepository,
        expirationRepository = expirationRepository,
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
    expirationRepository: ExpirationRepository? = null
) {
    val showPortalSelection = profile?.isAdmin == true && profile.isBeneficiary
    val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
    
    BeneficiaryPortalView(
        userName = displayName,
        showPortalSelection = showPortalSelection,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        onNavigateToOrders = { navController.navigate(Screen.RequestItems.route) },
        authRepository = authRepository,
        userRepository = userRepository,
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
    userRepository: UserRepository
) {
    val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
    
    NonBeneficiaryPortalView(
        userName = displayName,
        showPortalSelection = false,
        onPortalSelectionClick = { navController.navigate(Screen.PortalSelection.route) },
        authRepository = authRepository,
        userRepository = userRepository,
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
