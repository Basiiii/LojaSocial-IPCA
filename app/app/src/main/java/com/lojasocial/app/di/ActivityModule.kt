package com.lojasocial.app.di

import com.lojasocial.app.repository.activity.ActivityRepository
import com.lojasocial.app.repository.activity.ActivityRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing activity-related dependencies.
 * 
 * This module binds the ActivityRepository interface to its concrete implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityModule {

    /**
     * Binds the ActivityRepository interface to its implementation.
     * 
     * @param activityRepositoryImpl The concrete implementation of ActivityRepository
     * @return ActivityRepository The interface instance
     */
    @Binds
    @Singleton
    abstract fun bindActivityRepository(
        activityRepositoryImpl: ActivityRepositoryImpl
    ): ActivityRepository
}
