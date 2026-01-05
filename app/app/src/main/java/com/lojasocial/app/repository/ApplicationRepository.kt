package com.lojasocial.app.repository

import android.content.Context
import com.lojasocial.app.domain.Application
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {
    suspend fun submitApplication(application: Application): Result<String>
    suspend fun convertFileToBase64(uri: android.net.Uri): Result<String>
    fun getApplications(): Flow<List<Application>>
    suspend fun getApplicationById(id: String): Result<Application>
    fun setContext(context: Context)
}
