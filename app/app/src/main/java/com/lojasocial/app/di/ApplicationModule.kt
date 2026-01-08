package com.lojasocial.app.di

import com.lojasocial.app.repository.application.ApplicationRepository
import com.lojasocial.app.repository.application.ApplicationRepositoryImpl
import com.lojasocial.app.repository.user.ProfilePictureRepository
import com.lojasocial.app.repository.user.ProfilePictureRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing application-related dependencies.
 * 
 * This module is responsible for binding the ApplicationRepository interface
 * to its concrete implementation (ApplicationRepositoryImpl) using dependency injection.
 * 
 * @see ApplicationRepository The interface for application data operations
 * @see ApplicationRepositoryImpl The Firebase Firestore implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    /**
     * Binds the ApplicationRepository interface to its implementation.
     * 
     * This method tells Dagger Hilt to use ApplicationRepositoryImpl whenever
     * ApplicationRepository is requested as a dependency. The implementation
     * is provided as a singleton, ensuring only one instance exists throughout
     * the application lifecycle.
     * 
     * @param applicationRepositoryImpl The concrete implementation of ApplicationRepository
     * @return ApplicationRepository The interface instance
     */
    @Binds
    @Singleton
    abstract fun bindApplicationRepository(
        applicationRepositoryImpl: ApplicationRepositoryImpl
    ): ApplicationRepository
    
    /**
     * Binds the ProfilePictureRepository interface to its implementation.
     * 
     * This method tells Dagger Hilt to use ProfilePictureRepositoryImpl whenever
     * ProfilePictureRepository is requested as a dependency. The implementation
     * is provided as a singleton, ensuring only one instance exists throughout
     * the application lifecycle.
     * 
     * @param profilePictureRepositoryImpl The concrete implementation of ProfilePictureRepository
     * @return ProfilePictureRepository The interface instance
     */
    @Binds
    @Singleton
    abstract fun bindProfilePictureRepository(
        profilePictureRepositoryImpl: ProfilePictureRepositoryImpl
    ): ProfilePictureRepository
}
