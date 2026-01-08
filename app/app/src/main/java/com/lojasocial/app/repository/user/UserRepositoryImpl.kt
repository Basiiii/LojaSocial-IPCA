package com.lojasocial.app.repository.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val cachedProfile = MutableStateFlow<UserProfile?>(null)
    private var currentUid: String? = null

    override suspend fun getUserProfile(uid: String): Flow<UserProfile?> {
        // Clear cache if UID changed
        if (currentUid != null && currentUid != uid) {
            cachedProfile.value = null
        }
        
        // Always read from Firestore to ensure fresh data
        // Don't return cached data early - let the snapshot listener handle updates

        return callbackFlow {
            currentUid = uid
            val docRef = firestore.collection("users").document(uid)
            
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val data = snapshot?.data
                val profile = if (data != null) {
                    // Helper to safely get boolean (handles Boolean, Long, and other types from Firestore)
                    fun getBoolean(key: String): Boolean {
                        val value = data[key]
                        return when (value) {
                            is Boolean -> value
                            is Long -> value == 1L
                            is Number -> value.toInt() == 1
                            else -> false
                        }
                    }
                    
                    val mappedProfile = UserProfile(
                        uid = uid, // Use document ID as uid, not stored field
                        email = data["email"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        isAdmin = getBoolean("isAdmin"),
                        isBeneficiary = getBoolean("isBeneficiary"),
                        profilePicture = data["profilePicture"] as? String
                    )
                    cachedProfile.value = mappedProfile
                    mappedProfile
                } else {
                    null
                }

                trySend(profile)
            }
            
            awaitClose { listener.remove() }
        }
    }

    override suspend fun getCurrentUserProfile(): Flow<UserProfile?> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            getUserProfile(currentUser.uid)
        } else {
            kotlinx.coroutines.flow.flow { emit(null) }
        }
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            // Use update() instead of set() to preserve existing fields like fcmToken
            val updateData = mutableMapOf<String, Any>(
                "email" to profile.email,
                "name" to profile.name,
                "isAdmin" to profile.isAdmin,
                "isBeneficiary" to profile.isBeneficiary
            )
            // Only add profilePicture if it's not null
            profile.profilePicture?.let {
                updateData["profilePicture"] = it
            }
            
            firestore.collection("users").document(profile.uid)
                .update(updateData)
                .await()
            // Update cache
            cachedProfile.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProfile(profile: UserProfile): Result<Unit> {
        return try {
            // Convert to map excluding uid (uid is the document ID, not a field)
            val profileData = mutableMapOf<String, Any>(
                "email" to profile.email,
                "name" to profile.name,
                "isAdmin" to profile.isAdmin,
                "isBeneficiary" to profile.isBeneficiary
            )
            // Only add profilePicture if it's not null
            profile.profilePicture?.let {
                profileData["profilePicture"] = it
            }
            
            firestore.collection("users").document(profile.uid)
                .set(profileData)
                .await()
            // Update cache
            cachedProfile.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFcmToken(token: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
