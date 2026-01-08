package com.lojasocial.app.repository.user

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for profile picture operations.
 * 
 * This interface defines methods for uploading and retrieving user profile pictures.
 * Profile pictures are stored as Base64 strings in the user's profile document.
 */
interface ProfilePictureRepository {
    /**
     * Uploads a profile picture for a user.
     * 
     * @param uid The user ID
     * @param imageBase64 The Base64-encoded image string
     * @return Result indicating success or failure
     */
    suspend fun uploadProfilePicture(uid: String, imageBase64: String): Result<Unit>
    
    /**
     * Gets the profile picture for a user.
     * 
     * @param uid The user ID
     * @return Flow emitting the Base64-encoded image string, or null if no picture exists
     */
    suspend fun getProfilePicture(uid: String): Flow<String?>
}
