package com.lojasocial.app.repository.user

import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val isAdmin: Boolean = false,
    val isBeneficiary: Boolean = false,
    val profilePicture: String? = null,
    val absences: Int = 0
)

interface UserRepository {
    suspend fun getUserProfile(uid: String): Flow<UserProfile?>
    suspend fun getCurrentUserProfile(): Flow<UserProfile?>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun saveFcmToken(token: String): Result<Unit>
    
    /**
     * Retrieves all users who are beneficiaries (isBeneficiary == true).
     * 
     * @return Flow emitting lists of beneficiary user profiles
     */
    suspend fun getAllBeneficiaries(): Flow<List<UserProfile>>
}
