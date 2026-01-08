package com.lojasocial.app.repository.application

import android.content.Context
import com.lojasocial.app.domain.application.Application
import com.lojasocial.app.domain.application.ApplicationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing scholarship application data.
 * 
 * This interface defines the contract for application data operations,
 * providing a clean separation between the data layer and the business logic.
 * Implementations of this interface handle data persistence, retrieval,
 * and file operations for scholarship applications.
 * 
 * @see Application The domain model for scholarship applications
 * @see ApplicationRepositoryImpl The Firebase Firestore implementation
 */
interface ApplicationRepository {
    
    /**
     * Submits a new scholarship application to the data store.
     * 
     * This method handles the complete submission process, including
     * document conversion to Base64 format and storage in Firestore.
     * 
     * @param application The complete application to submit
     * @return Result containing the application ID if successful, or error if failed
     */
    suspend fun submitApplication(application: Application): Result<String>
    
    /**
     * Converts a file URI to Base64 encoded string for storage.
     * 
     * This method handles the conversion of local file URIs to Base64
     * strings that can be stored in Firestore documents.
     * 
     * @param uri The URI of the file to convert
     * @return Result containing the Base64 string if successful, or error if failed
     */
    suspend fun convertFileToBase64(uri: android.net.Uri): Result<String>
    
    /**
     * Retrieves all applications for the current user.
     * 
     * This method returns a Flow that emits updates to the application
     * list whenever changes occur in the data store, providing real-time
     * updates to the UI.
     * 
     * @return Flow emitting lists of applications for the current user
     */
    fun getApplications(): Flow<List<Application>>
    
    /**
     * Retrieves all applications from all users.
     * 
     * This method returns a Flow that emits updates to all applications
     * in the system, regardless of userId. This is typically used by
     * employees or administrators to review all applications.
     * 
     * @return Flow emitting lists of all applications
     */
    fun getAllApplications(): Flow<List<Application>>
    
    /**
     * Retrieves a specific application by its unique identifier.
     * 
     * This method fetches a single application from the data store
     * using its unique ID. It verifies that the application belongs
     * to the current user.
     * 
     * @param id The unique identifier of the application
     * @return Result containing the application if found, or error if not found
     */
    suspend fun getApplicationById(id: String): Result<Application>
    
    /**
     * Retrieves a specific application by its unique identifier without user verification.
     * 
     * This method fetches a single application from the data store using its unique ID
     * without checking if it belongs to the current user. This is typically used by
     * employees or administrators to view any application.
     * 
     * @param id The unique identifier of the application
     * @return Result containing the application if found, or error if not found
     */
    suspend fun getApplicationByIdForEmployee(id: String): Result<Application>
    
    /**
     * Updates the status of an application.
     * 
     * This method allows employees/administrators to update the status of an application
     * (e.g., approve or reject). Optionally includes a rejection message when rejecting.
     * 
     * @param applicationId The unique identifier of the application
     * @param status The new status to set
     * @param rejectionMessage Optional message to include when rejecting (null for approval)
     * @return Result indicating success or failure
     */
    suspend fun updateApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
        rejectionMessage: String? = null
    ): Result<Unit>
    
    /**
     * Sets the Android context for file operations.
     * 
     * This method provides the repository with the necessary context
     * for performing file operations such as reading file URIs and
     * converting them to Base64 format.
     * 
     * @param context The Android application context
     */
    fun setContext(context: Context)
    
    /**
     * Retrieves the approved application for a specific user (beneficiary).
     * 
     * This method fetches the application with APPROVED status for a given userId.
     * Used to display beneficiary application information.
     * 
     * @param userId The unique identifier of the beneficiary user
     * @return Result containing the approved application if found, or null if not found
     */
    suspend fun getBeneficiaryApplication(userId: String): Result<Application?>
}
