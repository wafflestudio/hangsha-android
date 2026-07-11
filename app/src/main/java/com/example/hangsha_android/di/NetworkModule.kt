package com.example.hangsha_android.di

import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.AuthTokenAuthenticator
import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.api.BookmarkApi
import com.example.hangsha_android.data.network.api.BugReportApi
import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.api.ExcludedKeywordsApi
import com.example.hangsha_android.data.network.api.MemoApi
import com.example.hangsha_android.data.network.api.ServerHealthApi
import com.example.hangsha_android.data.network.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    @Named("auth")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("app")
    fun provideAppOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authTokenStorage: AuthTokenStorage,
        authTokenAuthenticator: AuthTokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val path = originalRequest.url.encodedPath
                if (AUTH_PATH_PREFIXES.any(path::startsWith)) {
                    return@addInterceptor chain.proceed(originalRequest)
                }

                val accessToken = authTokenStorage.getAccessToken()
                val request = if (accessToken.isNullOrBlank()) {
                    originalRequest
                } else {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                }

                chain.proceed(request)
            }
            .authenticator(authTokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("app")
    fun provideAppRetrofit(
        @Named("app") okHttpClient: OkHttpClient
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
        @Named("app") retrofit: Retrofit
    ): ServerHealthApi {
        return retrofit.create(ServerHealthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("auth") retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(
        @Named("app") retrofit: Retrofit
    ): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideEventApi(
        @Named("app") retrofit: Retrofit
    ): EventApi {
        return retrofit.create(EventApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBookmarkApi(
        @Named("app") retrofit: Retrofit
    ): BookmarkApi {
        return retrofit.create(BookmarkApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExcludedKeywordsApi(
        @Named("app") retrofit: Retrofit
    ): ExcludedKeywordsApi {
        return retrofit.create(ExcludedKeywordsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBugReportApi(
        @Named("app") retrofit: Retrofit
    ): BugReportApi {
        return retrofit.create(BugReportApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMemoApi(
        @Named("app") retrofit: Retrofit
    ): MemoApi {
        return retrofit.create(MemoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCategoryApi(
        @Named("app") retrofit: Retrofit
    ): CategoryApi {
        return retrofit.create(CategoryApi::class.java)
    }

    private val AUTH_PATH_PREFIXES = listOf(
        "/api/v1/mobile/auth/",
        "/api/v1/auth/"
    )
}
