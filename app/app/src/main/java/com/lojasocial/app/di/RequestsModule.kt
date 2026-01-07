package com.lojasocial.app.di

import com.lojasocial.app.repository.RequestsRepository
import com.lojasocial.app.repository.RequestsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RequestsModule {

    @Binds
    @Singleton
    abstract fun bindRequestsRepository(
        requestsRepositoryImpl: RequestsRepositoryImpl
    ): RequestsRepository
}

