package com.lojasocial.app.repository

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
        // Return cached data if available and for same user (only if not null)
        if (currentUid == uid && cachedProfile.value != null) {
            return cachedProfile.asStateFlow()
        }

        return callbackFlow {
            currentUid = uid
            val docRef = firestore.collection("users").document(uid)
            
            // Debug logging
            println("DEBUG: Fetching user profile for UID: $uid")
            
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: Firestore error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                println("DEBUG: Snapshot data: ${snapshot?.data}")
                
                val profile = snapshot?.toObject(UserProfile::class.java)
                if (profile != null) {
                    cachedProfile.value = profile
                    println("DEBUG: Profile loaded: $profile")
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
            firestore.collection("users").document(profile.uid)
                .set(profile)
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
            firestore.collection("users").document(profile.uid)
                .set(profile)
                .await()
            // Update cache
            cachedProfile.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
