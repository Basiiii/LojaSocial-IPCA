package com.lojasocial.app.di

import com.lojasocial.app.repository.request.ItemsRepository
import com.lojasocial.app.repository.request.ItemsRepositoryImpl
import com.lojasocial.app.repository.request.OrdersRepository
import com.lojasocial.app.repository.request.OrdersRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RequestItemsModule {

    @Binds
    @Singleton
    abstract fun bindItemsRepository(
        itemsRepositoryImpl: ItemsRepositoryImpl
    ): ItemsRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        ordersRepositoryImpl: OrdersRepositoryImpl
    ): OrdersRepository
}
