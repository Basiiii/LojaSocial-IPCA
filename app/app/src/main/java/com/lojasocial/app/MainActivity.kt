package com.lojasocial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lojasocial.app.ui.beneficiaries.BeneficiaryPortalView
import com.lojasocial.app.ui.employees.EmployeePortalView
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

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
                                    BeneficiaryPortalView()
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
                                    BeneficiaryPortalView()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}