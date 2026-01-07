package com.lojasocial.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.data.model.Activity
import com.lojasocial.app.data.model.ActivityType
import com.lojasocial.app.data.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore implementation of ActivityRepository.
 * 
 * This implementation queries existing collections (requests and applications)
 * to derive activity items without requiring a separate activity log table.
 */
@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ActivityRepository {

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

    override fun getRecentActivitiesForBeneficiary(limit: Int): Flow<List<Activity>> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit(emptyList())
                return@flow
            }

            // Query user's requests ordered by submissionDate desc
            val requestsSnapshot = firestore.collection("requests")
                .whereEqualTo("userId", userId)
                .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val activities = requestsSnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val status = (data["status"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"]) ?: Date()
                    
                    val activityType = when (status) {
                        0 -> ActivityType.REQUEST_SUBMITTED
                        1 -> ActivityType.REQUEST_ACCEPTED
                        2 -> ActivityType.PICKUP_COMPLETED
                        else -> null
                    } ?: return@mapNotNull null

                    val (title, subtitle) = when (activityType) {
                        ActivityType.REQUEST_SUBMITTED -> Pair(
                            "Pedido Submetido",
                            "Pedido submetido e pendente"
                        )
                        ActivityType.REQUEST_ACCEPTED -> Pair(
                            "Pedido Aceite",
                            "O teu pedido foi aceite"
                        )
                        ActivityType.PICKUP_COMPLETED -> Pair(
                            "Levantamento Concluído",
                            "Levantamento feito com sucesso"
                        )
                        else -> return@mapNotNull null
                    }

                    Activity(
                        id = doc.id,
                        type = activityType,
                        title = title,
                        subtitle = subtitle,
                        timestamp = submissionDate
                    )
                } catch (e: Exception) {
                    null
                }
            }

            emit(activities)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getRecentActivitiesForEmployee(limit: Int): Flow<List<Activity>> = flow {
        try {
            val allActivities = mutableListOf<Activity>()

            // Query all requests
            val requestsSnapshot = firestore.collection("requests")
                .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            // Collect unique user IDs to batch fetch user names
            val userIds = requestsSnapshot.documents.mapNotNull { doc ->
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

            requestsSnapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val status = (data["status"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"]) ?: Date()
                    val userId = data["userId"] as? String
                    val userName = userId?.let { userNameMap[it] }

                    val activityType = when (status) {
                        0 -> ActivityType.REQUEST_SUBMITTED
                        1 -> ActivityType.REQUEST_ACCEPTED
                        2 -> ActivityType.PICKUP_COMPLETED
                        else -> null
                    } ?: return@forEach

                    val (title, subtitle) = when (activityType) {
                        ActivityType.REQUEST_SUBMITTED -> {
                            val name = userName ?: "Utilizador"
                            Pair("Pedido Submetido", "$name - Novo pedido")
                        }
                        ActivityType.REQUEST_ACCEPTED -> {
                            val name = userName ?: "Utilizador"
                            Pair("Pedido Aceite", name)
                        }
                        ActivityType.PICKUP_COMPLETED -> {
                            val name = userName ?: "Utilizador"
                            Pair("Levantamento Concluído", "$name - Alimentar")
                        }
                        else -> return@forEach
                    }

                    allActivities.add(
                        Activity(
                            id = doc.id,
                            type = activityType,
                            title = title,
                            subtitle = subtitle,
                            timestamp = submissionDate,
                            userId = userId,
                            userName = userName
                        )
                    )
                } catch (e: Exception) {
                    // Skip invalid documents
                }
            }

            // Query all applications from applications collection
            // Applications are stored in applications/{applicationId} with userId field
            val applicationsSnapshot = firestore.collection("applications")
                .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            applicationsSnapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val statusStr = data["status"] as? String ?: ApplicationStatus.PENDING.name
                    val status = try {
                        ApplicationStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        ApplicationStatus.PENDING
                    }
                    val submissionDate = convertToDate(data["submissionDate"]) ?: Date()
                    
                    // Get userId from document data
                    val userId = data["userId"] as? String

                    val personalInfoData = data["personalInfo"] as? Map<*, *>
                    val userName = personalInfoData?.get("name") as? String

                    val activityType = when (status) {
                        ApplicationStatus.PENDING -> ActivityType.APPLICATION_SUBMITTED
                        ApplicationStatus.APPROVED -> ActivityType.APPLICATION_APPROVED
                        else -> null
                    } ?: return@forEach

                    val (title, subtitle) = when (activityType) {
                        ActivityType.APPLICATION_SUBMITTED -> Pair(
                            "Nova Candidatura",
                            userName ?: "Nova candidatura submetida"
                        )
                        ActivityType.APPLICATION_APPROVED -> Pair(
                            "Candidatura Aceite",
                            userName ?: "Candidatura aprovada"
                        )
                        else -> return@forEach
                    }

                    allActivities.add(
                        Activity(
                            id = doc.id,
                            type = activityType,
                            title = title,
                            subtitle = subtitle,
                            timestamp = submissionDate,
                            userId = userId,
                            userName = userName
                        )
                    )
                } catch (e: Exception) {
                    // Skip invalid documents
                }
            }

            // Sort all activities by timestamp (most recent first) and limit
            val sortedActivities = allActivities
                .sortedByDescending { it.timestamp }
                .take(limit)

            emit(sortedActivities)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
