package com.example.hangsha_android

import android.app.Application
import android.util.Log
import com.example.hangsha_android.data.local.LocalDataMigration
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NidOAuth
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class HangshaApplication : Application() {
    @Inject
    lateinit var localDataMigration: LocalDataMigration

    override fun onCreate() {
        super.onCreate()

        runCatching {
            runBlocking { localDataMigration.runIfNeeded() }
        }.onFailure { error ->
            Log.w("HangshaApplication", "Local data migration failed.", error)
        }

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }

        if (
            BuildConfig.NAVER_CLIENT_ID.isNotBlank() &&
            BuildConfig.NAVER_CLIENT_SECRET.isNotBlank()
        ) {
            NidOAuth.initialize(
                context = this,
                clientId = BuildConfig.NAVER_CLIENT_ID,
                clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
                clientName = BuildConfig.NAVER_CLIENT_NAME
            )
        }
    }
}
