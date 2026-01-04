package com.lojasocial.app.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.data.model.Campaign
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val campaignsCollection = firestore.collection("campaigns")

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
                    val campaign = doc.toObject(Campaign::class.java)?.copy(id = doc.id)
                    Log.d("CampaignRepository", "Campaign: ${campaign?.name} (id: ${doc.id})")
                    campaign
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
}
