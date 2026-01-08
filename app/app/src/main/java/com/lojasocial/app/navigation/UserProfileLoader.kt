package com.lojasocial.app.navigation

import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.user.UserProfile
import com.lojasocial.app.repository.user.UserRepository
import kotlinx.coroutines.flow.first

/**
 * Result of loading user profile and determining navigation destination.
 */
data class NavigationStartState(
    val destination: String,
    val profile: UserProfile?,
    val error: String? = null
)

/**
 * Loads user profile and determines the appropriate start destination.
 * 
 * @param authRepository Authentication repository
 * @param userRepository User repository
 * @return NavigationStartState with destination, profile, and optional error
 */
suspend fun loadUserProfileAndDestination(
    authRepository: AuthRepository,
    userRepository: UserRepository
): NavigationStartState {
    return try {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            val profile = userRepository.getCurrentUserProfile().first()
            if (profile != null) {
                val destination = NavigationHelper.getDestinationForUser(profile)
                if (destination == Screen.Login.route) {
                    NavigationStartState(
                        destination = Screen.Login.route,
                        profile = profile,
                        error = "Perfil carregado mas sem portal válido."
                    )
                } else {
                    NavigationStartState(
                        destination = destination,
                        profile = profile
                    )
                }
            } else {
                NavigationStartState(
                    destination = Screen.Login.route,
                    profile = null
                )
            }
        } else {
            NavigationStartState(
                destination = Screen.Login.route,
                profile = null
            )
        }
    } catch (e: Exception) {
        NavigationStartState(
            destination = Screen.Login.route,
            profile = null,
            error = "Erro ao carregar perfil: ${e.message}"
        )
    }
}
