package com.lojasocial.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import com.lojasocial.app.ui.beneficiaries.BeneficiariesListView
import com.lojasocial.app.ui.beneficiaries.BeneficiaryDetailView
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.lojasocial.app.ui.beneficiaries.BeneficiaryPortalView
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
    profilePictureRepository: ProfilePictureRepository,
    activity: androidx.activity.ComponentActivity? = null,
    notificationRequestId: String? = null,
    notificationApplicationId: String? = null
) {
    // Handle navigation when app is already running and notification is clicked
    LaunchedEffect(activity, lastProfile, notificationRequestId, notificationApplicationId) {
        if (activity != null && lastProfile != null) {
            val currentIntent = activity.intent
            val screenExtra = currentIntent.getStringExtra("screen")
            val requestId = currentIntent.getStringExtra("requestId") ?: notificationRequestId
            val applicationId = currentIntent.getStringExtra("applicationId") ?: notificationApplicationId
            
            android.util.Log.d("NavigationGraph", "LaunchedEffect - screen: $screenExtra, applicationId: $applicationId, requestId: $requestId")
            
            // Wait a bit longer to ensure NavHost is ready
            kotlinx.coroutines.delay(500)
            
            // Check if NavHost is ready
            if (navController.currentBackStackEntry == null) {
                android.util.Log.w("NavigationGraph", "NavHost not ready yet, waiting...")
                kotlinx.coroutines.delay(500)
            }
            
            try {
                when (screenExtra) {
                    "expiringItems" -> {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != Screen.ExpiringItems.route) {
                            navController.navigate(Screen.ExpiringItems.route) {
                                popUpTo(0) { inclusive = false }
                            }
                        }
                    }
                    "applicationDetail" -> {
                        if (applicationId != null && applicationId.isNotBlank()) {
                            val route = Screen.ApplicationDetail.createRoute(applicationId)
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            android.util.Log.d("NavigationGraph", "Navigating to applicationDetail: $route, currentRoute: $currentRoute")
                            
                            try {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        // Pop to root but keep it in the stack
                                        popUpTo(0) { inclusive = false }
                                        // Prevent multiple instances
                                        launchSingleTop = true
                                    }
                                } else {
                                    android.util.Log.d("NavigationGraph", "Already on applicationDetail route, skipping navigation")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationGraph", "Error navigating to applicationDetail", e)
                            }
                        } else {
                            android.util.Log.w("NavigationGraph", "applicationDetail screen requested but applicationId is null or blank")
                        }
                    }
                    "requestDetails" -> {
                        if (requestId != null) {
                            // Navigate to pickup requests - the view will need to auto-select the request
                            navController.navigate(Screen.PickupRequests.route) {
                                popUpTo(0) { inclusive = false }
                            }
                            // Store requestId to be picked up by PickupRequestsView
                            // We'll use a shared preference or pass it through navigation
                        }
                    }
                    "beneficiaryPortal" -> {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute?.startsWith(Screen.BeneficiaryPortal.route) != true) {
                            navController.navigate(Screen.BeneficiaryPortal.route) {
                                popUpTo(0) { inclusive = false }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationGraph", "Error navigating from notification", e)
            }
        }
    }
    
    // Also check on resume in case intent was updated via onNewIntent
    DisposableEffect(activity) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME && lastProfile != null) {
                    val currentIntent = (source as? androidx.activity.ComponentActivity)?.intent
                    val screenExtra = currentIntent?.getStringExtra("screen")
                    val requestId = currentIntent?.getStringExtra("requestId")
                    val applicationId = currentIntent?.getStringExtra("applicationId")
                    
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        android.util.Log.d("NavigationGraph", "DisposableEffect ON_RESUME - screen: $screenExtra, applicationId: $applicationId, requestId: $requestId")
                        
                        kotlinx.coroutines.delay(500)
                        
                        // Check if NavHost is ready
                        if (navController.currentBackStackEntry == null) {
                            android.util.Log.w("NavigationGraph", "DisposableEffect - NavHost not ready yet, waiting...")
                            kotlinx.coroutines.delay(500)
                        }
                        
                        try {
                            when (screenExtra) {
                                "expiringItems" -> {
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    if (currentRoute != Screen.ExpiringItems.route) {
                                        navController.navigate(Screen.ExpiringItems.route) {
                                            popUpTo(0) { inclusive = false }
                                        }
                                    }
                                }
                                "applicationDetail" -> {
                                    if (applicationId != null && applicationId.isNotBlank()) {
                                        val route = Screen.ApplicationDetail.createRoute(applicationId)
                                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                                        android.util.Log.d("NavigationGraph", "DisposableEffect - Navigating to applicationDetail: $route, currentRoute: $currentRoute")
                                        
                                        try {
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo(0) { inclusive = false }
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                android.util.Log.d("NavigationGraph", "DisposableEffect - Already on applicationDetail route, skipping navigation")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NavigationGraph", "DisposableEffect - Error navigating to applicationDetail", e)
                                        }
                                    } else {
                                        android.util.Log.w("NavigationGraph", "DisposableEffect - applicationDetail screen requested but applicationId is null or blank")
                                    }
                                }
                                "requestDetails" -> {
                                    if (requestId != null) {
                                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                                        if (currentRoute != Screen.PickupRequests.route) {
                                            navController.navigate(Screen.PickupRequests.route) {
                                                popUpTo(0) { inclusive = false }
                                            }
                                        }
                                    }
                                }
                                "beneficiaryPortal" -> {
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    if (currentRoute?.startsWith(Screen.BeneficiaryPortal.route) != true) {
                                        navController.navigate(Screen.BeneficiaryPortal.route) {
                                            popUpTo(0) { inclusive = false }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NavigationGraph", "Error navigating from notification on resume", e)
                        }
                    }
                }
            }
        }
        
        activity?.lifecycle?.addObserver(observer)
        
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
        }
    }
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
            composable(Screen.EmployeePortal.Gestao.route) { backStackEntry ->
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
                    expirationRepository = expirationRepository,
                    requestsRepository = requestsRepository
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
                    expirationRepository = expirationRepository,
                    requestsRepository = requestsRepository
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
                    expirationRepository = expirationRepository,
                    requestsRepository = requestsRepository
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
                    expirationRepository = expirationRepository,
                    requestsRepository = requestsRepository
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

        // Urgent Request
        composable(Screen.UrgentRequest.route) {
            com.lojasocial.app.ui.urgentRequest.UrgentRequestView(
                onBackClick = { navController.navigateUp() },
                requestsRepository = requestsRepository,
                userRepository = userRepository,
                profilePictureRepository = profilePictureRepository,
                productRepository = null // Can be added if needed
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

        // All Applications List (for employees - shows all applications including their own, but disabled)
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
                currentUserId = currentUserId // Pass currentUserId to disable own applications
            )
        }

        // Application Detail
        composable(
            route = Screen.ApplicationDetail().route,
            arguments = listOf(
                navArgument("applicationId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            // Extract applicationId from route arguments - this should work with navArgument
            val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
            val route = backStackEntry.destination.route ?: ""
            
            android.util.Log.d("NavigationGraph", "ApplicationDetail composable - route: $route, applicationId: $applicationId")
            
            // Validate applicationId is not empty
            if (applicationId.isBlank()) {
                android.util.Log.e("NavigationGraph", "ApplicationDetail: applicationId is blank! Route: $route")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "ID da candidatura não fornecido",
                            color = androidx.compose.ui.graphics.Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Text("Voltar")
                        }
                    }
                }
            } else {
                // Get current user ID to check if this is their own application
                val currentUserId = authRepository.getCurrentUser()?.uid
                var applicationUserId by remember { mutableStateOf<String?>(null) }
                
                // Check if this application belongs to the current user
                LaunchedEffect(applicationId, currentUserId) {
                    if (applicationId.isNotBlank() && currentUserId != null) {
                        try {
                            val result = applicationRepository.getApplicationById(applicationId)
                            if (result.isSuccess) {
                                applicationUserId = result.getOrNull()?.userId
                            }
                        } catch (e: Exception) {
                            // If getApplicationById fails (e.g., not their application), try employee view
                            android.util.Log.d("NavigationGraph", "Could not get application as user, will try employee view")
                        }
                    }
                }
                
                // Determine view mode:
                // - If application belongs to current user, show as beneficiary view (even if admin)
                // - If came from AllApplicationsList, show as employee view
                // - If user is admin and it's not their application, show as employee view
                val isEmployeeViewFromRoute = navController.previousBackStackEntry?.destination?.route == Screen.AllApplicationsList.route
                val isOwnApplication = applicationUserId == currentUserId
                val isEmployeeView = when {
                    isOwnApplication -> false // Always show own applications as beneficiary view
                    isEmployeeViewFromRoute -> true
                    lastProfile?.isAdmin == true -> true
                    else -> false
                }
                
                android.util.Log.d("NavigationGraph", "ApplicationDetail - applicationId: $applicationId, currentUserId: $currentUserId, applicationUserId: $applicationUserId, isOwnApplication: $isOwnApplication, isEmployeeView: $isEmployeeView")
                
                // ApplicationDetailView has its own error handling, so we can safely call it
                ApplicationDetailView(
                    applicationId = applicationId,
                    applicationRepository = applicationRepository,
                    onNavigateBack = { navController.navigateUp() },
                    isEmployeeView = isEmployeeView
                )
            }
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

        // Stock List
        composable(Screen.StockList.route) {
            com.lojasocial.app.ui.stock.StockListView(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        val destination = when {
                            lastProfile?.isAdmin == true && lastProfile.isBeneficiary -> Screen.BeneficiaryPortal.Home.route
                            lastProfile?.isAdmin == true -> Screen.EmployeePortal.Home.route
                            else -> Screen.BeneficiaryPortal.Home.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.StockList.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToStockItems = { barcode ->
                    navController.navigate(Screen.StockItems.createRoute(barcode))
                }
            )
        }

        // Stock Items
        composable(Screen.StockItems().route) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
            com.lojasocial.app.ui.stock.StockItemsView(
                barcode = barcode,
                onNavigateBack = {
                    navController.popBackStack()
                }
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

        // Beneficiaries List
        composable(Screen.BeneficiariesList.route) {
            BeneficiariesListView(
                userRepository = userRepository,
                profilePictureRepository = profilePictureRepository,
                onNavigateBack = { navController.navigateUp() },
                onItemClick = { userId ->
                    navController.navigate(Screen.BeneficiaryDetail.createRoute(userId))
                }
            )
        }

        // Beneficiary Detail
        composable(Screen.BeneficiaryDetail().route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            BeneficiaryDetailView(
                userId = userId,
                userRepository = userRepository,
                applicationRepository = applicationRepository,
                profilePictureRepository = profilePictureRepository,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Pickup Requests
        composable(Screen.PickupRequests.route) {
            // Check if we're coming from beneficiary portal by checking previous destination
            val isFromBeneficiaryPortal = navController.previousBackStackEntry?.destination?.route?.startsWith(Screen.BeneficiaryPortal.route) == true
            val isFromEmployeePortal = navController.previousBackStackEntry?.destination?.route?.startsWith(Screen.EmployeePortal.route) == true
            
            // Get requestId from notification if available
            val requestIdFromNotification = remember { 
                activity?.intent?.getStringExtra("requestId") ?: notificationRequestId
            }
            
            // Check if the request belongs to the current user (for notifications)
            val currentUserId = authRepository.getCurrentUser()?.uid
            var requestUserId by remember { mutableStateOf<String?>(null) }
            var isOwnRequest by remember { mutableStateOf(false) }
            
            LaunchedEffect(requestIdFromNotification, currentUserId) {
                if (requestIdFromNotification != null && currentUserId != null && requestsRepository != null) {
                    try {
                        // Get the request directly by ID to check ownership
                        val result = requestsRepository.getRequestById(requestIdFromNotification)
                        if (result.isSuccess) {
                            val request = result.getOrNull()
                            requestUserId = request?.userId
                            isOwnRequest = request?.userId == currentUserId
                            android.util.Log.d("NavigationGraph", "PickupRequests - requestId: $requestIdFromNotification, currentUserId: $currentUserId, requestUserId: $requestUserId, isOwnRequest: $isOwnRequest")
                        } else {
                            android.util.Log.w("NavigationGraph", "Could not get request by ID: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NavigationGraph", "Error checking request ownership", e)
                    }
                }
            }
            
            // Determine if we should filter by current user:
            // - If coming from beneficiary portal, filter by current user
            // - If notification about own request, always show as beneficiary view (even if user is also admin)
            val shouldFilterByCurrentUser = when {
                isFromBeneficiaryPortal -> true
                isOwnRequest -> true // Own request notification, always show as beneficiary view
                else -> false
            }
            
            android.util.Log.d("NavigationGraph", "PickupRequests - shouldFilterByCurrentUser: $shouldFilterByCurrentUser, isFromBeneficiaryPortal: $isFromBeneficiaryPortal, isOwnRequest: $isOwnRequest")
            
            com.lojasocial.app.ui.requests.PickupRequestsView(
                onNavigateBack = {
                    // Navigate back to the portal we came from, or default based on profile
                    val portalRoute = when {
                        isFromBeneficiaryPortal -> Screen.BeneficiaryPortal.route
                        isFromEmployeePortal -> Screen.EmployeePortal.route
                        lastProfile?.isBeneficiary == true -> Screen.BeneficiaryPortal.route
                        lastProfile?.isAdmin == true -> Screen.EmployeePortal.route
                        else -> Screen.NonBeneficiaryPortal.route
                    }
                    navController.navigate(portalRoute) {
                        popUpTo(portalRoute) { inclusive = false }
                    }
                },
                userRepository = userRepository,
                requestsRepository = requestsRepository,
                profilePictureRepository = profilePictureRepository,
                filterByCurrentUser = shouldFilterByCurrentUser,
                initialRequestId = requestIdFromNotification
            )
        }

        // Weekly Pickups
        composable(Screen.WeeklyPickups.route) {
            com.lojasocial.app.ui.weeklypickups.WeeklyPickupsView(
                profilePictureRepository = profilePictureRepository,
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
                // Validate campaign ID before fetching
                if (campaignId.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ID da campanha inválido")
                    }
                } else {
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
                        campaign != null && campaign!!.id.isNotBlank() -> {
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
        onNavigateToWeeklyPickups = {
            navController.navigate(Screen.WeeklyPickups.route)
        },
        onNavigateToAuditLogs = {
            navController.navigate(Screen.AuditLogs.route)
        },
        onNavigateToBeneficiaries = {
            navController.navigate(Screen.BeneficiariesList.route)
        },
        onNavigateToStockList = {
            navController.navigate(Screen.StockList.route)
        },
        onNavigateToUrgentRequest = {
            navController.navigate(Screen.UrgentRequest.route)
        },
        currentTab = tab,
        onTabChange = { newTab ->
            val route = when (newTab) {
                "home" -> Screen.EmployeePortal.Home.route
                "profile" -> Screen.EmployeePortal.Profile.route
                "support" -> Screen.EmployeePortal.Support.route
                "calendar" -> Screen.EmployeePortal.Calendar.route
                "gestao" -> Screen.EmployeePortal.Gestao.route
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
    expirationRepository: ExpirationRepository? = null,
    requestsRepository: RequestsRepository? = null
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
        requestsRepository = requestsRepository,
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
