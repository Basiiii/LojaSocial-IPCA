package com.lojasocial.app.di

import com.lojasocial.app.data.repository.PendingRequestsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePendingRequestsRepository(): PendingRequestsRepository {
        return PendingRequestsRepository()
    }
}