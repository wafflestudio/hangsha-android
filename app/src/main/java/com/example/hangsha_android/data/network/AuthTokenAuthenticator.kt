package com.example.hangsha_android.data.network

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.model.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class AuthTokenAuthenticator @Inject constructor(
    private val authTokenStorage: AuthTokenStorage,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (isAuthRequest(response.request) || responseCount(response) >= MAX_AUTH_RETRY_COUNT) {
            return null
        }

        val requestAccessToken = response.request.header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: return null

        synchronized(this) {
            val latestAccessToken = authTokenStorage.getAccessToken()
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$latestAccessToken")
                    .build()
            }

            val refreshToken = authTokenStorage.getRefreshToken()
                ?.takeUnless { it.isBlank() }
                ?: run {
                    authTokenStorage.clearTokens()
                    return null
                }

            val refreshResponse = runBlocking {
                authApi.refresh(
                    RefreshTokenRequest(refreshToken = refreshToken)
                )
            }
            if (!refreshResponse.isSuccessful) {
                authTokenStorage.clearTokens()
                return null
            }

            val refreshedTokens = refreshResponse.body()
            val newAccessToken = refreshedTokens?.accessToken
            val newRefreshToken = refreshedTokens?.refreshToken
            if (newAccessToken.isNullOrBlank() || newRefreshToken.isNullOrBlank()) {
                authTokenStorage.clearTokens()
                return null
            }

            authTokenStorage.saveRefreshedTokens(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )

            return response.request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newAccessToken")
                .build()
        }
    }

    private fun isAuthRequest(request: Request): Boolean {
        val path = request.url.encodedPath
        return AUTH_PATH_PREFIXES.any(path::startsWith)
    }

    private fun responseCount(response: Response): Int {
        var currentResponse: Response? = response
        var count = 1
        while (currentResponse?.priorResponse != null) {
            count += 1
            currentResponse = currentResponse.priorResponse
        }
        return count
    }

    private companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val MAX_AUTH_RETRY_COUNT = 2
        private val AUTH_PATH_PREFIXES = listOf(
            "/api/v1/mobile/auth/",
            "/api/v1/auth/"
        )
    }
}
