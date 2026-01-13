package com.lojasocial.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.lojasocial.app.navigation.NavigationGraph
import com.lojasocial.app.navigation.loadUserProfileAndDestination
import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.repository.product.ExpirationRepository
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.repository.user.UserRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.utils.FcmTokenService
import com.lojasocial.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var applicationRepository: ApplicationRepository

    @Inject
    lateinit var expirationRepository: ExpirationRepository

    @Inject
    lateinit var campaignRepository: CampaignRepository

    @Inject
    lateinit var requestsRepository: RequestsRepository

    @Inject
    lateinit var profilePictureRepository: ProfilePictureRepository

    @Inject
    lateinit var fcmTokenService: FcmTokenService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link from notification
        val screen = intent.getStringExtra("screen")
        val notificationType = intent.getStringExtra("type")
        val requestId = intent.getStringExtra("requestId")
        val applicationId = intent.getStringExtra("applicationId")
        val shouldNavigateToExpiringItems = screen == "expiringItems"
        val shouldNavigateToApplicationDetail = screen == "applicationDetail" && applicationId != null
        val shouldNavigateToRequestDetails = screen == "requestDetails" && requestId != null
        val shouldNavigateToBeneficiaryPortal = screen == "beneficiaryPortal"
        
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

                    // Request notification permission for Android 13+
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            Log.d("MainActivity", "Notification permission granted")
                        } else {
                            Log.w("MainActivity", "Notification permission denied")
                        }
                    }

                    // Request notification permission on app start
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (!hasPermission) {
                                Log.d("MainActivity", "Requesting notification permission")
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Load user profile and determine start destination
                    LaunchedEffect(Unit) {
                        val state = loadUserProfileAndDestination(authRepository, userRepository)
                        lastProfile.value = state.profile
                        navigationError.value = state.error
                        startDestination = state.destination
                        
                        // Get FCM token after login
                        if (state.profile != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                fcmTokenService.getAndStoreToken()
                            }
                        }
                        
                        // Navigate based on notification type
                        // Note: startDestination must be a simple route, not parameterized
                        // We'll handle navigation to parameterized routes after NavHost is created
                        if (state.profile != null) {
                            when {
                                shouldNavigateToExpiringItems -> {
                                    startDestination = Screen.ExpiringItems.route
                                }
                                shouldNavigateToApplicationDetail -> {
                                    startDestination = state.destination
                                }
                                shouldNavigateToRequestDetails -> {
                                    // Navigate to pickup requests, will auto-select request
                                    startDestination = Screen.PickupRequests.route
                                }
                                shouldNavigateToBeneficiaryPortal -> {
                                    startDestination = Screen.BeneficiaryPortal.route
                                }
                            }
                        }
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
                            applicationRepository = applicationRepository,
                            expirationRepository = expirationRepository,
                            campaignRepository = campaignRepository,
                            requestsRepository = requestsRepository,
                            profilePictureRepository = profilePictureRepository,
                            activity = this@MainActivity,
                            notificationRequestId = requestId,
                            notificationApplicationId = applicationId
                        )
                        
                        // Handle navigation to parameterized routes after NavHost is created
                        if (shouldNavigateToApplicationDetail && applicationId != null) {
                            LaunchedEffect(navController) {
                                kotlinx.coroutines.delay(100) // Small delay to ensure NavHost is ready
                                try {
                                    val route = Screen.ApplicationDetail.createRoute(applicationId)
                                    navController.navigate(route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error navigating to application detail", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val screen = intent?.getStringExtra("screen")
        val applicationId = intent?.getStringExtra("applicationId")
        val requestId = intent?.getStringExtra("requestId")
        Log.d("MainActivity", "onNewIntent called - screen: $screen, applicationId: $applicationId, requestId: $requestId")
    }
}
