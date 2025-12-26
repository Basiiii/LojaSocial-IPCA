package com.lojasocial.app.ui.beneficiaries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.beneficiaries.ActivitySection
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.profile.ProfileView

@Composable
fun BeneficiaryPortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository
) {
    var selectedTab by remember { mutableStateOf("home") }

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
                        name = userName?.takeIf { it.isNotBlank() } ?: "Carla",
                        message = "Recebe apoio quando precisares e acompanha os teus pedidos"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionsSection()

                    Spacer(modifier = Modifier.height(24.dp))

                    ActivitySection()

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            "profile" -> {
                ProfileView(
                    paddingValues = paddingValues,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    onLogout = { }
                )
            }

            "support" -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Suporte (por implementar)")
                }
            }

            "calendar" -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Calendário (por implementar)")
                }
            }
        }
    }

    if (useAppLayout) {
        AppLayout(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            subtitle = "Portal Beneficiários",
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun BeneficiaryPreview() {
    MaterialTheme {
        val mockAuthRepository = object : com.lojasocial.app.repository.AuthRepository {
            override suspend fun signIn(email: String, password: String) = TODO()
            override suspend fun signUp(email: String, password: String) = TODO()
            override suspend fun signOut() = TODO()
            override fun getCurrentUser() = TODO()
            override fun isUserLoggedIn() = TODO()
        }

        val mockUserRepository = object : com.lojasocial.app.repository.UserRepository {
            override suspend fun getUserProfile(uid: String) = kotlinx.coroutines.flow.flow {
                emit(
                    com.lojasocial.app.repository.UserProfile(
                        uid = "preview",
                        email = "preview@lojasocial.pt",
                        name = "Preview User",
                        isAdmin = false,
                        isBeneficiary = true
                    )
                )
            }

            override suspend fun getCurrentUserProfile() = kotlinx.coroutines.flow.flow {
                emit(
                    com.lojasocial.app.repository.UserProfile(
                        uid = "preview",
                        email = "preview@lojasocial.pt",
                        name = "Preview User",
                        isAdmin = false,
                        isBeneficiary = true
                    )
                )
            }

            override suspend fun updateProfile(profile: com.lojasocial.app.repository.UserProfile) = TODO()
            override suspend fun createProfile(profile: com.lojasocial.app.repository.UserProfile) = TODO()
        }

        BeneficiaryPortalView(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository
        )
    }
}
