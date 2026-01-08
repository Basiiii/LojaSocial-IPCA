package com.lojasocial.app.di

import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.audit.AuditRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuditModule {

    @Binds
    @Singleton
    abstract fun bindAuditRepository(
        auditRepositoryImpl: AuditRepositoryImpl
    ): AuditRepository
}
