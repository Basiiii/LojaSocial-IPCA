package com.lojasocial.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.repository.campaign.CampaignRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CampaignModule {

    @Provides
    @Singleton
    fun provideCampaignRepository(firestore: FirebaseFirestore): CampaignRepository {
        return CampaignRepository(firestore)
    }
}
