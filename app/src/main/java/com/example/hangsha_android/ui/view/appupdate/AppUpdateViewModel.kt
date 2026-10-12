package com.example.hangsha_android.ui.view.appupdate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.network.model.AppUpdateType
import com.example.hangsha_android.data.repository.AppVersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appVersionRepository: AppVersionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    fun checkUpdatePolicy(availableVersionCode: Int) {
        if (
            availableVersionCode <= BuildConfig.VERSION_CODE ||
            checkJob?.isActive == true
        ) {
            return
        }

        _uiState.update { it.copy(isCheckingPolicy = true, prompt = null) }
        checkJob = viewModelScope.launch {
            try {
                val response = appVersionRepository.getAppVersion(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    availableVersionCode = availableVersionCode
                )
                val prompt = when (response.updateType) {
                    AppUpdateType.NONE -> null
                    AppUpdateType.OPTIONAL,
                    AppUpdateType.REQUIRED -> AppUpdatePromptUiState(
                        isRequired = response.updateType == AppUpdateType.REQUIRED,
                        latestVersionName = response.latestVersionName,
                        releaseNotes = response.releaseNotes
                    )
                }
                _uiState.update {
                    it.copy(
                        isCheckingPolicy = false,
                        prompt = prompt
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "App update policy request failed.", error)
                publishCheckFailure()
            }
        }
    }

    fun reportPlayUpdateCheckFailure(error: Throwable) {
        Log.w(TAG, "Google Play update check failed.", error)
        publishCheckFailure()
    }

    fun onToastMessageConsumed() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun cancelCheckAndClearPrompt() {
        checkJob?.cancel()
        checkJob = null
        _uiState.update {
            it.copy(
                isCheckingPolicy = false,
                prompt = null
            )
        }
    }

    private fun publishCheckFailure() {
        _uiState.update {
            it.copy(
                isCheckingPolicy = false,
                prompt = null,
                toastMessage = UPDATE_CHECK_FAILED_MESSAGE
            )
        }
    }

    private companion object {
        const val TAG = "AppUpdateViewModel"
        const val UPDATE_CHECK_FAILED_MESSAGE =
            "업데이트 정보를 확인하지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}
