package com.lojasocial.app.repository.campaign

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.campaign.Campaign
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val campaignsCollection = firestore.collection("campaigns")
    
    /**
     * Helper function to convert Firestore Timestamp to Date.
     */
    private fun convertTimestampToDate(value: Any?): Date? {
        return when {
            value is Date -> value
            value is Timestamp -> value.toDate()
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

    suspend fun getActiveAndRecentCampaigns(): List<Campaign> {
        return try {
            Log.d("CampaignRepository", "Fetching campaigns from Firestore...")
            
            val oneMonthAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }.time
            
            Log.d("CampaignRepository", "Filtering campaigns with endDate >= $oneMonthAgo")

            val snapshot = campaignsCollection
                .whereGreaterThanOrEqualTo("endDate", oneMonthAgo)
                .orderBy("endDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d("CampaignRepository", "Got ${snapshot.documents.size} documents from Firestore")
            
            val campaigns = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val name = data["name"] as? String ?: ""
                    val startDate = convertTimestampToDate(data["startDate"])
                    val endDate = convertTimestampToDate(data["endDate"])
                    
                    if (startDate != null && endDate != null) {
                        Campaign(
                            id = doc.id,
                            name = name,
                            startDate = startDate,
                            endDate = endDate
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("CampaignRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d("CampaignRepository", "Successfully loaded ${campaigns.size} campaigns")
            campaigns
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error fetching campaigns: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getAllCampaigns(): List<Campaign> {
        return try {
            // Try with orderBy first, fallback to unordered if index doesn't exist
            val snapshot = try {
                campaignsCollection
                    .orderBy("startDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails (index missing), get without ordering
                Log.w("CampaignRepository", "OrderBy failed, fetching without order: ${e.message}")
                campaignsCollection.get().await()
            }
            
            val campaigns = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val name = data["name"] as? String ?: ""
                    val startDate = convertTimestampToDate(data["startDate"])
                    val endDate = convertTimestampToDate(data["endDate"])
                    
                    if (startDate != null && endDate != null) {
                        Campaign(
                            id = doc.id,
                            name = name,
                            startDate = startDate,
                            endDate = endDate
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("CampaignRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Sort manually if we couldn't use orderBy
            campaigns.sortedByDescending { it.startDate }
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error fetching all campaigns: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Fetches campaigns with pagination support.
     * @param limit Maximum number of campaigns to fetch
     * @param lastStartDate The startDate of the last campaign from the previous page (for cursor-based pagination)
     * @return Pair of (campaigns list, hasMore) where hasMore indicates if there are more campaigns to load
     */
    suspend fun getCampaignsPaginated(
        limit: Int = 15,
        lastStartDate: Date? = null
    ): Pair<List<Campaign>, Boolean> {
        return try {
            var query = campaignsCollection
                .orderBy("startDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            
            // If we have a lastStartDate, start after it
            if (lastStartDate != null) {
                query = query.startAfter(Timestamp(lastStartDate))
            }
            
            val snapshot = try {
                query.limit(limit.toLong() + 1) // Fetch one extra to check if there are more
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails (index missing), fallback to simple pagination
                Log.w("CampaignRepository", "OrderBy pagination failed, using fallback: ${e.message}")
                if (lastStartDate == null) {
                    campaignsCollection.limit(limit.toLong() + 1).get().await()
                } else {
                    // For fallback, we'll need to fetch all and filter client-side
                    // This is not ideal but works if index is missing
                    val allSnapshot = campaignsCollection.get().await()
                    val allCampaigns = allSnapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val name = data["name"] as? String ?: ""
                            val startDate = convertTimestampToDate(data["startDate"])
                            val endDate = convertTimestampToDate(data["endDate"])
                            
                            if (startDate != null && endDate != null) {
                                Campaign(
                                    id = doc.id,
                                    name = name,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e("CampaignRepository", "Error parsing document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    val sorted = allCampaigns.sortedByDescending { it.startDate }
                    val startIndex = sorted.indexOfFirst { it.startDate <= lastStartDate }
                    val endIndex = if (startIndex == -1) 0 else startIndex
                    val page = sorted.subList(endIndex, minOf(endIndex + limit + 1, sorted.size))
                    val hasMore = page.size > limit
                    val result = if (hasMore) page.dropLast(1) else page
                    return Pair(result, hasMore)
                }
            }
            
            val hasMore = snapshot.documents.size > limit
            val documentsToProcess = if (hasMore) snapshot.documents.dropLast(1) else snapshot.documents
            
            val campaigns = documentsToProcess.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val name = data["name"] as? String ?: ""
                    val startDate = convertTimestampToDate(data["startDate"])
                    val endDate = convertTimestampToDate(data["endDate"])
                    
                    if (startDate != null && endDate != null) {
                        Campaign(
                            id = doc.id,
                            name = name,
                            startDate = startDate,
                            endDate = endDate
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("CampaignRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Pair(campaigns, hasMore)
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error fetching paginated campaigns: ${e.message}", e)
            Pair(emptyList(), false)
        }
    }
    
    suspend fun updateCampaign(campaign: Campaign): Result<Unit> {
        return try {
            val campaignData = hashMapOf(
                "name" to campaign.name,
                "startDate" to Timestamp(campaign.startDate),
                "endDate" to Timestamp(campaign.endDate)
            )
            
            campaignsCollection.document(campaign.id)
                .set(campaignData)
                .await()
            
            Log.d("CampaignRepository", "Campaign updated successfully: ${campaign.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error updating campaign: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        return try {
            campaignsCollection.document(campaignId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error deleting campaign: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun createCampaign(campaign: Campaign): Result<String> {
        return try {
            val campaignData = hashMapOf(
                "name" to campaign.name,
                "startDate" to Timestamp(campaign.startDate),
                "endDate" to Timestamp(campaign.endDate)
            )
            
            Log.d("CampaignRepository", "Creating campaign: ${campaign.name}, startDate: ${campaign.startDate}, endDate: ${campaign.endDate}")
            
            val docRef = campaignsCollection.add(campaignData).await()
            
            Log.d("CampaignRepository", "Campaign created successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("CampaignRepository", "Error creating campaign: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
