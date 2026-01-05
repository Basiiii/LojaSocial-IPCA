package com.lojasocial.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
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
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    // Helper function to determine portal based on user roles
    private fun getDestinationForUser(userProfile: UserProfile?): String {
        return when {
            userProfile == null -> "login" // No profile means not logged in
            userProfile.isAdmin && userProfile.isBeneficiary -> "portalSelection"
            !userProfile.isAdmin && !userProfile.isBeneficiary -> "nonBeneficiaryPortal"
            userProfile.isAdmin -> "employeePortal"
            userProfile.isBeneficiary -> "beneficiaryPortal"
            else -> "login" // No roles means login
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LojaSocialTheme(
                darkTheme = false
            ) {
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
                            Log.d(
                                "LojaSocialDebug",
                                "DEBUG_MAIN: LaunchedEffect currentUser=$currentUser"
                            )
                            if (currentUser != null) {
                                val profile = userRepository.getCurrentUserProfile().first()
                                Log.d(
                                    "LojaSocialDebug",
                                    "DEBUG_MAIN: LaunchedEffect collected profile=$profile"
                                )
                                if (profile != null) {
                                    lastProfile.value = profile
                                    val destination = getDestinationForUser(profile)
                                    Log.d(
                                        "LojaSocialDebug",
                                        "DEBUG_MAIN: LaunchedEffect startDestination=$destination with profile=$profile"
                                    )
                                    if (destination == "login") {
                                        navigationError.value =
                                            "Perfil carregado mas sem portal válido: isAdmin=${profile.isAdmin}, isBeneficiário=${profile.isBeneficiary}. Contacta o SAS."
                                        startDestination = "login"
                                    } else {
                                        startDestination = destination
                                    }
                                } else {
                                    Log.d(
                                        "LojaSocialDebug",
                                        "DEBUG_MAIN: LaunchedEffect profile is null despite logged-in user"
                                    )
                                    startDestination = "login"
                                }
                            } else {
                                // No current user, go to login
                                startDestination = "login"
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "LojaSocialDebug",
                                "DEBUG_MAIN: LaunchedEffect error: ${e.message}"
                            )
                            startDestination = "login"
                        }
                    }

                    if (startDestination == null) {
                        // Simple loading state while we determine where to start
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        ) {
                            composable("employeePortal") {
                                // Show portal selection tab only if user has both roles
                                val profile = lastProfile.value
                                val showPortalSelection =
                                    profile?.isAdmin == true && profile.isBeneficiary
                                val displayName = profile?.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?.substringBefore(" ")
                                    ?: "Utilizador"
                                EmployeePortalView(
                                    userName = displayName,
                                    showPortalSelection = showPortalSelection,
                                    onPortalSelectionClick = {
                                        navController.navigate("portalSelection")
                                    },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("beneficiaryPortal") {
                                // Show portal selection tab only if user has both roles
                                val profile = lastProfile.value
                                val showPortalSelection =
                                    profile?.isAdmin == true && profile.isBeneficiary
                                val displayName = profile?.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?.substringBefore(" ")
                                    ?: "Utilizador"
                                BeneficiaryPortalView(
                                    userName = displayName,
                                    showPortalSelection = showPortalSelection,
                                    onPortalSelectionClick = {
                                        navController.navigate("portalSelection")
                                    },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("portalSelection") {
                                val profile = lastProfile.value
                                val displayName = profile?.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?.substringBefore(" ")
                                    ?: "Utilizador"
                                PortalSelectionView(
                                    userName = displayName,
                                    onNavigateToEmployeePortal = {
                                        navController.navigate("employeePortal")
                                    },
                                    onNavigateToBeneficiaryPortal = {
                                        navController.navigate("beneficiaryPortal")
                                    },
                                    onLogout = {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            authRepository.signOut()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                            composable("nonBeneficiaryPortal"){
                                val profile = lastProfile.value
                                val displayName = profile?.name
                                    ?.takeIf { it.isNotBlank() }
                                    ?.substringBefore(" ")
                                    ?: "Utilizador"
                                NonBeneficiaryPortalView(
                                    userName = displayName,
                                    showPortalSelection = false,
                                    onPortalSelectionClick = {
                                        navController.navigate("portalSelection")
                                    },
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onNavigateToApplication = {
                                        navController.navigate("applicationFlow")
                                    },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            navigation(
                                startDestination = "applicationPage1", //TODO: Change to the new names
                                route = "applicationFlow"
                            ) {
                                composable("applicationPage1") {
                                    val parentEntry = remember {
                                        navController.currentBackStackEntry
                                    }
                                    val viewModel: com.lojasocial.app.ui.viewmodel.ApplicationViewModel = 
                                        androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = parentEntry!!)
                                    CandidaturaPersonalInfoView(
                                        navController = navController,
                                        onNavigateNext = {
                                            navController.navigate("applicationPage2")
                                        },
                                        viewModel = viewModel
                                    )
                                }
                                composable("applicationPage2") {
                                    val parentEntry = remember {
                                        navController.currentBackStackEntry
                                    }
                                    val viewModel: com.lojasocial.app.ui.viewmodel.ApplicationViewModel = 
                                        androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = parentEntry!!)
                                    CandidaturaAcademicDataView(
                                        onNavigateBack = {
                                            navController.navigateUp()
                                        },
                                        onNavigateNext = {
                                            navController.navigate("applicationPage3")
                                        },
                                        viewModel = viewModel
                                    )
                                }
                                composable("applicationPage3") {
                                    val parentEntry = remember {
                                        navController.currentBackStackEntry
                                    }
                                    val viewModel: com.lojasocial.app.ui.viewmodel.ApplicationViewModel = 
                                        androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = parentEntry!!)
                                    CandidaturaDocumentsView(
                                        onNavigateBack = {
                                            navController.navigateUp()
                                        },
                                        onSubmit = {
                                            // Handle form submission - navigate back to nonBeneficiaryPortal
                                            navController.navigate("nonBeneficiaryPortal") {
                                                popUpTo("nonBeneficiaryPortal") { inclusive = false }
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
                                        // After login, fetch profile once and navigate based on roles
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
                                                navigationError.value = null
                                                val profile =
                                                    userRepository.getCurrentUserProfile().first()
                                                Log.d(
                                                    "LojaSocialDebug",
                                                    "DEBUG_MAIN: onLoginSuccess collected profile=$profile"
                                                )
                                                if (profile != null) {
                                                    lastProfile.value = profile
                                                    val destination = getDestinationForUser(profile)
                                                    Log.d(
                                                        "LojaSocialDebug",
                                                        "DEBUG_MAIN: onLoginSuccess navigating to $destination with profile=$profile"
                                                    )
                                                    if (destination == "login") {
                                                        // Roles don't allow any portal, show error with actual flags
                                                        navigationError.value =
                                                            "Perfil carregado mas sem portal válido: isAdmin=${profile.isAdmin}, isBeneficiário=${profile.isBeneficiary}. Contacta o SAS."
                                                    } else {
                                                        navController.navigate(destination) {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    navigationError.value =
                                                        "Não foi possível carregar o teu perfil. Tenta novamente ou contacta o SAS."
                                                }
                                            } catch (e: Exception) {
                                                navigationError.value =
                                                    "Erro ao carregar o teu perfil. Tenta novamente mais tarde."
                                                Log.d(
                                                    "LojaSocialDebug",
                                                    "DEBUG_MAIN: onLoginSuccess error: ${e.message}"
                                                )
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

    @Composable
    fun CalendarView(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Calendar",
                modifier = Modifier.size(64.dp),
                tint = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Calendário",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Em breve disponível",
                fontSize = 16.sp,
                color = TextGray
            )
        }
    }
}