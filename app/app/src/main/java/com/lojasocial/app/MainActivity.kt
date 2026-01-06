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
import androidx.navigation.compose.rememberNavController
import com.lojasocial.app.navigation.NavigationGraph
import com.lojasocial.app.navigation.loadUserProfileAndDestination
import com.lojasocial.app.repository.ApplicationRepository
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.repository.UserProfile
import com.lojasocial.app.ui.theme.LojaSocialTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var applicationRepository: ApplicationRepository

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

                    // Load user profile and determine start destination
                    LaunchedEffect(Unit) {
                        val state = loadUserProfileAndDestination(authRepository, userRepository)
                        lastProfile.value = state.profile
                        navigationError.value = state.error
                        startDestination = state.destination
                    }

                    // Show loading indicator while determining start destination
                    if (startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavigationGraph(
                            navController = navController,
                            startDestination = startDestination!!,
                            lastProfile = lastProfile.value,
                            navigationError = navigationError.value,
                            onNavigationErrorChange = { navigationError.value = it },
                            onProfileChange = { lastProfile.value = it },
                            authRepository = authRepository,
                            userRepository = userRepository,
                            applicationRepository = applicationRepository
                        )
                    }
                }
            }
        }
    }
}
