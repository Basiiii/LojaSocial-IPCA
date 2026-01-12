package com.lojasocial.app.di

import android.util.Log
import com.lojasocial.app.BuildConfig
import com.lojasocial.app.api.AuditApiService
import com.lojasocial.app.api.ExpirationApiService
import com.lojasocial.app.api.ImageApiService
import com.lojasocial.app.api.NotificationApiService
import com.lojasocial.app.api.ProductApiService
import com.lojasocial.app.utils.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url
            
            Log.d("ApiModule", "=== API REQUEST ===")
            Log.d("ApiModule", "Full URL: $url")
            Log.d("ApiModule", "Method: ${originalRequest.method}")
            Log.d("ApiModule", "Headers: ${originalRequest.headers}")
            
            val newRequest = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
                
            Log.d("ApiModule", "Making request to: ${newRequest.url}")
            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val baseUrl = BuildConfig.BACKEND_URL
        Log.d("ApiModule", "Creating Retrofit with base URL: $baseUrl")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideProductApiService(retrofit: Retrofit): ProductApiService {
        return retrofit.create(ProductApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideExpirationApiService(okHttpClient: OkHttpClient): ExpirationApiService {
        val expirationRetrofit = Retrofit.Builder()
            .baseUrl(AppConstants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return expirationRetrofit.create(ExpirationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuditApiService(okHttpClient: OkHttpClient): AuditApiService {
        val auditRetrofit = Retrofit.Builder()
            .baseUrl(AppConstants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return auditRetrofit.create(AuditApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImageApiService(okHttpClient: OkHttpClient): ImageApiService {
        val imageRetrofit = Retrofit.Builder()
            .baseUrl(AppConstants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return imageRetrofit.create(ImageApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApiService(okHttpClient: OkHttpClient): NotificationApiService {
        val notificationRetrofit = Retrofit.Builder()
            .baseUrl(AppConstants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return notificationRetrofit.create(NotificationApiService::class.java)
    }
}
