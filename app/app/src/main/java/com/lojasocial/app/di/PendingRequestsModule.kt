package com.lojasocial.app.di

import com.lojasocial.app.repository.PendingRequestsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PendingRequestsModule {

    @Provides
    @Singleton
    fun providePendingRequestsRepository(): PendingRequestsRepository {
        return PendingRequestsRepository()
    }
}