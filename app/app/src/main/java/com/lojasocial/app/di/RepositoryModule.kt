package com.lojasocial.app.di

import com.lojasocial.app.repository.RequestItemsRepository
import com.lojasocial.app.repository.RequestItemsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRequestItemsRepository(
        requestItemsRepositoryImpl: RequestItemsRepositoryImpl
    ): RequestItemsRepository
}
