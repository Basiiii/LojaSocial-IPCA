package com.lojasocial.app.repository.request

import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.domain.request.RequestItemDetail
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for managing pickup requests.
 */
interface RequestsRepository {
    /**
     * Gets all pickup requests for employees (all requests).
     * For beneficiaries, this should be filtered by userId.
     */
    fun getAllRequests(): Flow<List<Request>>
    
    /**
     * Gets all pickup requests for the current authenticated user.
     * This method filters requests by userId at the Firestore query level,
     * which is necessary for beneficiaries who can only read their own requests.
     */
    fun getRequests(): Flow<List<Request>>
    
    /**
     * Fetches requests with pagination support.
     * @param limit Maximum number of requests to fetch
     * @param lastSubmissionDate The submissionDate of the last request from the previous page (for cursor-based pagination)
     * @return Pair of (requests list, hasMore) where hasMore indicates if there are more requests to load
     */
    suspend fun getRequestsPaginated(
        limit: Int = 15,
        lastSubmissionDate: Date? = null
    ): Pair<List<Request>, Boolean>
    
    /**
     * Gets a single request by ID with all details including items.
     */
    suspend fun getRequestById(requestId: String): Result<Request>
    
    /**
     * Accepts a request and sets the scheduled pickup date.
     * Updates status to 1 (PENDENTE_LEVANTAMENTO) and sets scheduledPickupDate.
     */
    suspend fun acceptRequest(requestId: String, scheduledPickupDate: Date): Result<Unit>
    
    /**
     * Rejects a request with an optional reason.
     * Updates status to 3 (REJEITADO) and sets rejectionReason.
     */
    suspend fun rejectRequest(requestId: String, reason: String?): Result<Unit>
    
    /**
     * Gets the FCM token for a user by their userId.
     */
    suspend fun getUserFcmToken(userId: String): Result<String?>
    
    /**
     * Gets user profile data (name, email) by userId.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfileData>
    
    /**
     * Submits a new request with selected items.
     * Creates a request document with an items map (document ID -> quantity).
     */
    suspend fun submitRequest(selectedItems: Map<String, Int>): Result<Unit>
    
    /**
     * Completes a request (status 2 = CONCLUIDO).
     * Decreases both quantity and reservedQuantity for all items in the request.
     */
    suspend fun completeRequest(requestId: String): Result<Unit>
}

/**
 * Simple data class for user profile information.
 */
data class UserProfileData(
    val name: String = "",
    val email: String = ""
)
