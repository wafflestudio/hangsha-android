package com.example.hangsha_android.di

import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.api.ServerHealthApi
import com.example.hangsha_android.data.network.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authTokenStorage: AuthTokenStorage
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val accessToken = authTokenStorage.getAccessToken()
                val request = if (accessToken.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request()
                        .newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                }

                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideServerHealthApi(
        retrofit: Retrofit
    ): ServerHealthApi {
        return retrofit.create(ServerHealthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(
        retrofit: Retrofit
    ): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideEventApi(
        retrofit: Retrofit
    ): EventApi {
        return retrofit.create(EventApi::class.java)
    }
}
