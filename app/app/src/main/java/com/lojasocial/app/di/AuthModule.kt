package com.lojasocial.app.di

import com.google.firebase.auth.FirebaseAuth
import com.lojasocial.app.repository.AuthRepository
import com.lojasocial.app.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            // Firebase Auth persistence is enabled by default
            // No need to call setPersistence as it's not available in standard Firebase Auth
            return FirebaseAuth.getInstance()
        }
    }
}
