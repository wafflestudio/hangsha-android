package com.example.hangsha_android.ui.view.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.LocalDataMigration
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.repository.AuthRepository
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository,
    private val localDataMigration: LocalDataMigration
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            resetLocalDataIfNeeded()
            verifyRefreshToken()
        }
    }

    private suspend fun resetLocalDataIfNeeded() {
        try {
            localDataMigration.runIfNeeded()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(TAG, "Local data migration failed.", error)
        }
    }

    private suspend fun verifyRefreshToken() {
        val refreshToken = authTokenStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            navigateTo(SplashNavigationTarget.Login)
            return
        }

        val target = runCatching {
            refreshSession(refreshToken)
        }.fold(
            onSuccess = { SplashNavigationTarget.Calendar },
            onFailure = {
                authTokenStorage.clearTokens()
                SplashNavigationTarget.Login
            }
        )

        navigateTo(target)
    }

    private suspend fun refreshSession(refreshToken: String) {
        val response = authRepository.refresh(refreshToken)
        saveTokensFromResponse(response)
        activateCurrentUser()
        loadOrganizationNames()
        loadExcludedKeywords()
    }

    private suspend fun activateCurrentUser() {
        val response = userRepository.getMyProfile()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val userId = response.body()?.id
            ?: throw IllegalStateException("Profile response did not include a user ID.")
        authTokenStorage.setCurrentUserId(userId)
    }

    private fun saveTokensFromResponse(response: Response<LoginResponse>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val authTokens = response.body()
        val accessToken = authTokens?.accessToken
        val refreshToken = authTokens?.refreshToken
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            throw IllegalStateException("Authentication response did not include auth tokens.")
        }

        authTokenStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private suspend fun loadOrganizationNames() {
        runCatching {
            categoryRepository.ensureCategoryCatalogLoaded()
        }
    }

    private suspend fun loadExcludedKeywords() {
        runCatching {
            excludedKeywordsRepository.refreshExcludedKeywords()
        }
    }

    private fun navigateTo(target: SplashNavigationTarget) {
        _uiState.update {
            it.copy(navigationTarget = target)
        }
    }

    private companion object {
        const val TAG = "SplashViewModel"
    }
}
