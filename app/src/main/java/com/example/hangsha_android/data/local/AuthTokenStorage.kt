package com.example.hangsha_android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthTokenStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = createEncryptedSharedPreferences(context)
    private val _isLoggedIn = MutableStateFlow(!getAccessToken().isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        _isLoggedIn.value = accessToken.isNotBlank()
    }

    fun saveAccessToken(accessToken: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
        _isLoggedIn.value = accessToken.isNotBlank()
    }

    fun saveRefreshToken(refreshToken: String) {
        sharedPreferences.edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun hasAccessToken(): Boolean {
        return !getAccessToken().isNullOrBlank()
    }

    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        _isLoggedIn.value = false
    }

    fun clearAccessToken() {
        clearTokens()
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            buildEncryptedSharedPreferences(context)
        } catch (error: GeneralSecurityException) {
            recreateEncryptedSharedPreferences(context)
        } catch (error: IOException) {
            recreateEncryptedSharedPreferences(context)
        }
    }

    private fun recreateEncryptedSharedPreferences(context: Context): SharedPreferences {
        context.deleteSharedPreferences(FILE_NAME)
        return buildEncryptedSharedPreferences(context)
    }

    private fun buildEncryptedSharedPreferences(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val FILE_NAME = "auth_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}