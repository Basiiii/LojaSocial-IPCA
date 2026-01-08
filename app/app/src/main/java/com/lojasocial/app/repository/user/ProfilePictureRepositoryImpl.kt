package com.lojasocial.app.repository.user

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProfilePictureRepository.
 * 
 * This class handles profile picture operations by directly updating
 * the profilePicture field in Firestore without affecting other fields.
 */
@Singleton
class ProfilePictureRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : ProfilePictureRepository {
    
    override suspend fun uploadProfilePicture(uid: String, imageBase64: String): Result<Unit> {
        return try {
            // Update only the profilePicture field, leaving other fields unchanged
            firestore.collection("users")
                .document(uid)
                .update("profilePicture", imageBase64)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProfilePicture(uid: String): Flow<String?> {
        return userRepository.getUserProfile(uid).map { profile ->
            profile?.profilePicture
        }
    }
}
