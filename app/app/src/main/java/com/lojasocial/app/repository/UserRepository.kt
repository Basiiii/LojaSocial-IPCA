package com.lojasocial.app.repository

import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val isAdmin: Boolean = false,
    val isBeneficiary: Boolean = false
)

interface UserRepository {
    suspend fun getUserProfile(uid: String): Flow<UserProfile?>
    suspend fun getCurrentUserProfile(): Flow<UserProfile?>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun saveFcmToken(token: String): Result<Unit>
}
