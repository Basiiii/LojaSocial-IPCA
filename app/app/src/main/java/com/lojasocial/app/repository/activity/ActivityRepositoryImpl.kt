package com.lojasocial.app.repository.activity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.activity.Activity
import com.lojasocial.app.domain.activity.ActivityType
import com.lojasocial.app.domain.application.ApplicationStatus
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
                android.util.Log.w("ActivityRepository", "getRecentActivitiesForBeneficiary: User not authenticated")
                emit(emptyList())
                return@flow
            }

            val allActivities = mutableListOf<Activity>()

            // Query user's requests ordered by submissionDate desc
            // Try with orderBy first, fallback to unordered if index doesn't exist
            val requestsSnapshot = try {
                firestore.collection("requests")
                    .whereEqualTo("userId", userId)
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit((limit * 2).toLong()) // Get more to have enough after combining with applications
                    .get()
                    .await()
            } catch (e: Exception) {
                android.util.Log.w("ActivityRepository", "getRecentActivitiesForBeneficiary: OrderBy query failed, trying without order: ${e.message}")
                // If orderBy fails (index missing), try without ordering
                try {
                    firestore.collection("requests")
                        .whereEqualTo("userId", userId)
                        .limit((limit * 2).toLong())
                        .get()
                        .await()
                } catch (e2: Exception) {
                    android.util.Log.e("ActivityRepository", "getRecentActivitiesForBeneficiary: Query failed: ${e2.message}", e2)
                    null
                }
            }

            if (requestsSnapshot != null) {
                android.util.Log.d("ActivityRepository", "getRecentActivitiesForBeneficiary: Found ${requestsSnapshot.documents.size} requests for user $userId")

                requestsSnapshot.documents.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val status = (data["status"] as? Long)?.toInt() ?: 0
                        val submissionDate = convertToDate(data["submissionDate"]) ?: Date()
                        
                        val activityType = when (status) {
                            0 -> ActivityType.REQUEST_SUBMITTED
                            1 -> ActivityType.REQUEST_ACCEPTED
                            2 -> ActivityType.PICKUP_COMPLETED
                            4 -> ActivityType.REQUEST_REJECTED
                            else -> null
                        } ?: return@forEach

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
                            ActivityType.REQUEST_REJECTED -> Pair(
                                "Pedido Rejeitado",
                                "O teu pedido foi rejeitado"
                            )
                            else -> return@forEach
                        }

                        allActivities.add(
                            Activity(
                                id = doc.id,
                                type = activityType,
                                title = title,
                                subtitle = subtitle,
                                timestamp = submissionDate
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid documents
                    }
                }
            }

            // Query user's applications
            val applicationsSnapshot = try {
                firestore.collection("applications")
                    .whereEqualTo("userId", userId)
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit((limit * 2).toLong()) // Get more applications to have enough after combining
                    .get()
                    .await()
            } catch (e: Exception) {
                android.util.Log.w("ActivityRepository", "getRecentActivitiesForBeneficiary: Applications orderBy query failed, trying without order: ${e.message}")
                // If orderBy fails, try without ordering
                try {
                    firestore.collection("applications")
                        .whereEqualTo("userId", userId)
                        .limit((limit * 2).toLong())
                        .get()
                        .await()
                } catch (e2: Exception) {
                    android.util.Log.e("ActivityRepository", "getRecentActivitiesForBeneficiary: Applications query failed: ${e2.message}", e2)
                    null
                }
            }

            if (applicationsSnapshot != null) {
                android.util.Log.d("ActivityRepository", "getRecentActivitiesForBeneficiary: Found ${applicationsSnapshot.documents.size} applications for user $userId")

                applicationsSnapshot.documents.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val statusValue = data["status"]
                        val status = when (statusValue) {
                            is String -> ApplicationStatus.fromString(statusValue)
                            is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                            is Int -> ApplicationStatus.fromInt(statusValue)
                            else -> ApplicationStatus.PENDING
                        }
                        val submissionDate = convertToDate(data["submissionDate"]) ?: Date()

                        val activityType = when (status) {
                            ApplicationStatus.PENDING -> ActivityType.APPLICATION_SUBMITTED
                            ApplicationStatus.APPROVED -> ActivityType.APPLICATION_APPROVED
                            ApplicationStatus.REJECTED -> ActivityType.APPLICATION_REJECTED
                            else -> null
                        } ?: return@forEach

                        val (title, subtitle) = when (activityType) {
                            ActivityType.APPLICATION_SUBMITTED -> Pair(
                                "Candidatura Submetida",
                                "A tua candidatura foi submetida"
                            )
                            ActivityType.APPLICATION_APPROVED -> Pair(
                                "Candidatura Aceite",
                                "A tua candidatura foi aceite"
                            )
                            ActivityType.APPLICATION_REJECTED -> Pair(
                                "Candidatura Rejeitada",
                                "A tua candidatura foi rejeitada"
                            )
                            else -> return@forEach
                        }

                        allActivities.add(
                            Activity(
                                id = doc.id,
                                type = activityType,
                                title = title,
                                subtitle = subtitle,
                                timestamp = submissionDate
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid documents
                    }
                }
            }

            // Sort all activities by timestamp (most recent first) and limit
            val sortedActivities = allActivities
                .sortedByDescending { it.timestamp }
                .take(limit)

            android.util.Log.d("ActivityRepository", "getRecentActivitiesForBeneficiary: Emitting ${sortedActivities.size} activities (total found: ${allActivities.size})")
            emit(sortedActivities)
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "getRecentActivitiesForBeneficiary: Exception: ${e.message}", e)
            emit(emptyList())
        }
    }

    override fun getRecentActivitiesForEmployee(limit: Int): Flow<List<Activity>> = flow {
        try {
            val allActivities = mutableListOf<Activity>()

            // Query all requests - use a larger limit to get more data before combining with applications
            val requestsSnapshot = try {
                firestore.collection("requests")
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit((limit * 2).toLong()) // Get more requests to have enough after combining
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails, try without ordering
                try {
                    firestore.collection("requests")
                        .limit((limit * 2).toLong())
                        .get()
                        .await()
                } catch (e2: Exception) {
                    null
                }
            }

            if (requestsSnapshot != null) {
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
                        4 -> ActivityType.REQUEST_REJECTED
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
                        ActivityType.REQUEST_REJECTED -> {
                            val name = userName ?: "Utilizador"
                            Pair("Pedido Rejeitado", name)
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
            }

            // Query all applications from applications collection
            // Applications are stored in applications/{applicationId} with userId field
            val applicationsSnapshot = try {
                firestore.collection("applications")
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit((limit * 2).toLong()) // Get more applications to have enough after combining
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails, try without ordering
                try {
                    firestore.collection("applications")
                        .limit((limit * 2).toLong())
                        .get()
                        .await()
                } catch (e2: Exception) {
                    null
                }
            }

            if (applicationsSnapshot != null) {
                applicationsSnapshot.documents.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val statusValue = data["status"]
                        val status = when (statusValue) {
                            is String -> ApplicationStatus.fromString(statusValue)
                            is Long -> ApplicationStatus.fromInt(statusValue.toInt())
                            is Int -> ApplicationStatus.fromInt(statusValue)
                            else -> ApplicationStatus.PENDING
                        }
                        val submissionDate = convertToDate(data["submissionDate"]) ?: Date()
                        
                        // Get userId from document data
                        val userId = data["userId"] as? String

                        val personalInfoData = data["personalInfo"] as? Map<*, *>
                        val userName = personalInfoData?.get("name") as? String

                        val activityType = when (status) {
                            ApplicationStatus.PENDING -> ActivityType.APPLICATION_SUBMITTED
                            ApplicationStatus.APPROVED -> ActivityType.APPLICATION_APPROVED
                            ApplicationStatus.REJECTED -> ActivityType.APPLICATION_REJECTED
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
                            ActivityType.APPLICATION_REJECTED -> Pair(
                                "Candidatura Rejeitada",
                                userName ?: "Candidatura rejeitada"
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
