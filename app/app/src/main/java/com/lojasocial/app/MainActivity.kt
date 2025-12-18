package com.lojasocial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.theme.TextDark
import com.lojasocial.app.ui.theme.TextGray
import com.lojasocial.app.ui.login.LoginScreen
import com.lojasocial.app.ui.profile.ProfileView
import com.lojasocial.app.ui.employees.EmployeePortalView
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lojasocial.app.ui.components.AppLayout
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

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
                    val isLoggedIn = authRepository.isUserLoggedIn().collectAsStateWithLifecycle(initialValue = null)
                    val navController = rememberNavController()
                    
                    when (isLoggedIn.value) {
                        null -> {
                            // Show loading spinner while checking auth state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        true -> {
                            // User is logged in, navigate to employee portal
                            NavHost(navController = navController, startDestination = "employeePortal") {
                                composable("employeePortal") {
                                    EmployeePortalWrapper(
                                        authRepository = authRepository,
                                        userRepository = userRepository,
                                        onLogout = {
                                            navController.navigate("login") {
                                                popUpTo("employeePortal") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        false -> {
                            // User is not logged in, show login screen
                            NavHost(navController = navController, startDestination = "login") {
                                composable("login") {
                                    LoginScreen(
                                        onLoginSuccess = {
                                            navController.navigate("employeePortal") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("employeePortal") {
                                    EmployeePortalWrapper(
                                        authRepository = authRepository,
                                        userRepository = userRepository,
                                        onLogout = {
                                            navController.navigate("login") {
                                                popUpTo("employeePortal") { inclusive = true }
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
}

@Composable
fun EmployeePortalWrapper(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("home") }
    
    AppLayout(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        subtitle = when (selectedTab) {
            "home" -> "Portal Funcionários"
            "profile" -> "Perfil"
            "support" -> "Suporte"
            "calendar" -> "Calendário"
            else -> "Portal Funcionários"
        }
    ) { paddingValues ->
        when (selectedTab) {
            "home" -> EmployeePortalView(paddingValues)
            "profile" -> ProfileView(paddingValues, authRepository, userRepository, onLogout)
            "support" -> SupportView(paddingValues)
            "calendar" -> CalendarView(paddingValues)
            else -> EmployeePortalView(paddingValues)
        }
    }
}

@Composable
fun SupportView(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Help,
            contentDescription = "Support",
            modifier = Modifier.size(64.dp),
            tint = TextGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Suporte",
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