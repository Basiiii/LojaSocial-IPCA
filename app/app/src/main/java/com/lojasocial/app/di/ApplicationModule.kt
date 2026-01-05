package com.lojasocial.app.di

import com.lojasocial.app.repository.ApplicationRepository
import com.lojasocial.app.repository.ApplicationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    @Binds
    @Singleton
    abstract fun bindApplicationRepository(
        applicationRepositoryImpl: ApplicationRepositoryImpl
    ): ApplicationRepository
}
