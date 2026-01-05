package com.lojasocial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.ui.applications.CandidaturaPersonalInfoView
import com.lojasocial.app.ui.applications.CandidaturaAcademicDataView
import com.lojasocial.app.ui.applications.CandidaturaDocumentsView
import com.lojasocial.app.ui.beneficiaries.BeneficiaryPortalView
import com.lojasocial.app.ui.employees.EmployeePortalView
import com.lojasocial.app.ui.login.LoginScreen
import com.lojasocial.app.ui.nonbeneficiaries.NonBeneficiaryPortalView
import com.lojasocial.app.ui.portalselection.PortalSelectionView
import com.lojasocial.app.ui.requestitems.RequestItemsView
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.viewmodel.ApplicationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

    @AndroidEntryPoint
    class MainActivity : ComponentActivity() {

        @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    private fun getDestinationForUser(userProfile: UserProfile?): String {
        return when {
            userProfile == null -> "login"
            userProfile.isAdmin && userProfile.isBeneficiary -> "portalSelection"
            !userProfile.isAdmin && !userProfile.isBeneficiary -> "nonBeneficiaryPortal"
            userProfile.isAdmin -> "employeePortal"
            else -> "beneficiaryPortal"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LojaSocialTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navigationError = remember { mutableStateOf<String?>(null) }
                    val lastProfile = remember { mutableStateOf<UserProfile?>(null) }
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        try {
                            val currentUser = authRepository.getCurrentUser()
                            if (currentUser != null) {
                                val profile = userRepository.getCurrentUserProfile().first()
                                if (profile != null) {
                                    lastProfile.value = profile
                                    val destination = getDestinationForUser(profile)
                                    if (destination == "login") {
                                        navigationError.value = "Perfil carregado mas sem portal válido."
                                        startDestination = "login"
                                    } else {
                                        startDestination = destination
                                    }
                                } else {
                                    startDestination = "login"
                                }
                            } else {
                                startDestination = "login"
                            }
                        } catch (e: Exception) {
                            startDestination = "login"
                        }
                    }

                    if (startDestination == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        ) {
                            composable("employeePortal") {
                                val profile = lastProfile.value
                                val showPortalSelection = profile?.isAdmin == true && profile.isBeneficiary
                                val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
                                EmployeePortalView(
                                    userName = displayName,
                                    showPortalSelection = showPortalSelection,
                                    onPortalSelectionClick = { navController.navigate("portalSelection") },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onLogout = {
                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }

                            composable("beneficiaryPortal") {
                                val profile = lastProfile.value
                                val showPortalSelection = profile?.isAdmin == true && profile.isBeneficiary
                                val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
                                BeneficiaryPortalView(
                                    userName = displayName,
                                    showPortalSelection = showPortalSelection,
                                    onPortalSelectionClick = { navController.navigate("portalSelection") },
                                    onNavigateToOrders = { navController.navigate("requestItems") },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onLogout = {
                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }

                            composable("requestItems") {
                                RequestItemsView(
                                    onBackClick = { navController.navigateUp() },
                                    onSubmitClick = {
                                        navController.navigate("beneficiaryPortal") {
                                            popUpTo("beneficiaryPortal") { inclusive = false }
                                        }
                                    }
                                )
                            }

                            composable("portalSelection") {
                                val profile = lastProfile.value
                                val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
                                PortalSelectionView(
                                    userName = displayName,
                                    onNavigateToEmployeePortal = { navController.navigate("employeePortal") },
                                    onNavigateToBeneficiaryPortal = { navController.navigate("beneficiaryPortal") },
                                    onLogout = {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            authRepository.signOut()
                                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                        }
                                    }
                                )
                            }

                            composable("nonBeneficiaryPortal") {
                                val profile = lastProfile.value
                                val displayName = profile?.name?.substringBefore(" ") ?: "Utilizador"
                                NonBeneficiaryPortalView(
                                    userName = displayName,
                                    showPortalSelection = false, // Usually false for non-beneficiaries
                                    onPortalSelectionClick = { navController.navigate("portalSelection") },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onNavigateToApplication = {
                                        navController.navigate("applicationFlow")
                                    },
                                    onLogout = {
                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }

                            navigation(
                                startDestination = "applicationPage1",
                                route = "applicationFlow"
                            ) {
                                composable("applicationPage1") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("applicationFlow")
                                    }
                                    val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                                    CandidaturaPersonalInfoView(
                                        onNavigateBack = { navController.navigate("nonBeneficiaryPortal") },
                                        onNavigateNext = { navController.navigate("applicationPage2") },
                                        viewModel = viewModel
                                    )
                                }

                                composable("applicationPage2") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("applicationFlow")
                                    }
                                    val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                                    CandidaturaAcademicDataView(
                                        onNavigateBack = { navController.navigate("applicationPage1") },
                                        onNavigateNext = { navController.navigate("applicationPage3") },
                                        viewModel = viewModel
                                    )
                                }

                                composable("applicationPage3") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("applicationFlow")
                                    }
                                    val viewModel = hiltViewModel<ApplicationViewModel>(parentEntry)

                                    CandidaturaDocumentsView(
                                        onNavigateBack = { navController.navigate("applicationPage2") },
                                        onSubmit = {
                                            navController.navigate("nonBeneficiaryPortal") {
                                                popUpTo("applicationFlow") { inclusive = true }
                                            }
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }

                            composable("login") {
                                LoginScreen(
                                    externalError = navigationError.value,
                                    onLoginSuccess = {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
                                                navigationError.value = null
                                                val profile = userRepository.getCurrentUserProfile().first()
                                                if (profile != null) {
                                                    lastProfile.value = profile
                                                    val destination = getDestinationForUser(profile)
                                                    if (destination == "login") {
                                                        navigationError.value = "Perfil inválido."
                                                    } else {
                                                        navController.navigate(destination) {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    navigationError.value = "Erro ao carregar perfil."
                                                }
                                            } catch (e: Exception) {
                                                navigationError.value = "Erro de conexão."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}