package com.lojasocial.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.repository.StockItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StockItemModule {

    @Provides
    @Singleton
    fun provideStockItemRepository(firestore: FirebaseFirestore): StockItemRepository {
        return StockItemRepository(firestore)
    }
}
