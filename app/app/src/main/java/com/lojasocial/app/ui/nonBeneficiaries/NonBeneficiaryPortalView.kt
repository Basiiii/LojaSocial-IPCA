package com.lojasocial.app.ui.nonbeneficiaries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.UserRepository
import com.lojasocial.app.ui.components.AppLayout
import com.lojasocial.app.ui.components.GreetingSection
import com.lojasocial.app.ui.profile.ProfileView

@Composable
fun NonBeneficiaryPortalView(
    useAppLayout: Boolean = true,
    userName: String? = null,
    showPortalSelection: Boolean = false,
    onPortalSelectionClick: (() -> Unit)? = null,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onCandidaturaClick: () -> Unit = {},
    onNavigateToApplication: () -> Unit = {}
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
                        name = userName?.takeIf { it.isNotBlank() } ?: "Utilizador",
                        message = "Realiza a tua candidatura para aceder aos benefícios disponíveis"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionsSection(onCandidaturaClick = onNavigateToApplication)

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
            subtitle = "Portal Candidatos",
            showPortalSelection = showPortalSelection,
            onPortalSelectionClick = onPortalSelectionClick
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Composable
fun QuickActionsSection(
    onCandidaturaClick: () -> Unit
) {
    CandidaturaCard(onClick = onCandidaturaClick)
}

@Composable
fun CandidaturaCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Realizar Candidatura",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Realiza a tua candidatura para aceder aos benefícios disponíveis",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = CardBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Realizar Candidatura",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Cor Azul Vibrante (Baseada no print)
val CardBlue = Color(0xFF2979FF)

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun NonBeneficiaryPreview() {
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
                        isBeneficiary = false
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
                        isBeneficiary = false
                    )
                )
            }

            override suspend fun updateProfile(profile: com.lojasocial.app.repository.UserProfile) = TODO()
            override suspend fun createProfile(profile: com.lojasocial.app.repository.UserProfile) = TODO()
        }

        NonBeneficiaryPortalView(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository
        )
    }
}