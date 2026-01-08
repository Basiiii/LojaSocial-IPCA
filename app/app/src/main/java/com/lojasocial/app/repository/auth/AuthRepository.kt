package com.lojasocial.app.repository.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthResult>
    suspend fun signUp(email: String, password: String): Result<AuthResult>
    suspend fun signOut()
    fun getCurrentUser(): FirebaseUser?
    fun isUserLoggedIn(): Flow<Boolean>
}
