package com.lojasocial.app.repository.calendar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.request.PickupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for calendar-related operations, specifically fetching pickup requests by date.
 */
@Singleton
class CalendarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    /**
     * Helper function to convert Firestore Timestamp to Date.
     */
    private fun convertToDate(value: Any?): Date? {
        return when {
            value is Date -> value
            value != null -> {
                val className = value.javaClass.name
                if (className == "com.google.firebase.Timestamp" || 
                    className == "com.google.firebase.firestore.Timestamp") {
                    try {
                        val toDateMethod = value.javaClass.getMethod("toDate")
                        toDateMethod.invoke(value) as? Date
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Gets all pickup requests (status = 2) for a specific date.
     * 
     * @param date The date to query pickup requests for
     * @param isEmployee If true, fetches all requests. If false, fetches only current user's requests.
     * @return Flow of list of pickup requests for the specified date
     */
    fun getPickupRequestsForDate(date: Date, isEmployee: Boolean = false): Flow<List<PickupRequest>> = flow {
        try {
            // Create start and end of day for date filtering
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.time
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.time
            
            // Build query - status 2 means pickup completed
            var query = firestore.collection("requests")
                .whereEqualTo("status", 2)
                .whereGreaterThanOrEqualTo("submissionDate", startOfDay)
                .whereLessThan("submissionDate", endOfDay)
            
            // If not employee, filter by current user
            if (!isEmployee) {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    emit(emptyList())
                    return@flow
                }
                query = query.whereEqualTo("userId", userId)
            }
            
            val snapshot = query.get().await()
            
            // Collect unique user IDs to batch fetch user names
            val userIds = snapshot.documents.mapNotNull { doc ->
                doc.data?.get("userId") as? String
            }.distinct()
            
            // Batch fetch user names
            val userNameMap = mutableMapOf<String, String>()
            userIds.forEach { uid ->
                try {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    val name = userDoc.data?.get("name") as? String
                    if (name != null) {
                        userNameMap[uid] = name
                    }
                } catch (e: Exception) {
                    // Skip if user not found
                }
            }
            
            // Map documents to PickupRequest
            val pickupRequests = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val userId = data["userId"] as? String ?: return@mapNotNull null
                    val totalItems = (data["totalItems"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"]) ?: return@mapNotNull null
                    val userName = userNameMap[userId]
                    
                    PickupRequest(
                        id = doc.id,
                        userId = userId,
                        userName = userName,
                        totalItems = totalItems,
                        pickupDate = submissionDate
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(pickupRequests)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * Gets pickup requests for a date range (for calendar month view).
     * Returns a map of date to count of pickups for that date.
     * 
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param isEmployee If true, fetches all requests. If false, fetches only current user's requests.
     * @return Flow of map where key is date (day only) and value is count of pickups
     */
    fun getPickupCountsForDateRange(
        startDate: Date,
        endDate: Date,
        isEmployee: Boolean = false
    ): Flow<Map<Date, Int>> = flow {
        try {
            // Build query - status 2 means pickup completed
            var query = firestore.collection("requests")
                .whereEqualTo("status", 2)
                .whereGreaterThanOrEqualTo("submissionDate", startDate)
                .whereLessThanOrEqualTo("submissionDate", endDate)
            
            // If not employee, filter by current user
            if (!isEmployee) {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    emit(emptyMap())
                    return@flow
                }
                query = query.whereEqualTo("userId", userId)
            }
            
            val snapshot = query.get().await()
            
            // Group by date (day only, ignoring time)
            val calendar = Calendar.getInstance()
            val dateCountMap = mutableMapOf<Date, Int>()
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val submissionDate = convertToDate(data["submissionDate"]) ?: return@forEach
                    
                    // Normalize to start of day
                    calendar.time = submissionDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val normalizedDate = calendar.time
                    
                    dateCountMap[normalizedDate] = dateCountMap.getOrDefault(normalizedDate, 0) + 1
                } catch (e: Exception) {
                    // Skip invalid documents
                }
            }
            
            emit(dateCountMap)
        } catch (e: Exception) {
            emit(emptyMap())
        }
    }
}

