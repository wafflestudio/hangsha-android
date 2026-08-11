package com.example.hangsha_android.ui.view.serverhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.api.ServerHealthApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerHealthViewModel @Inject constructor(
    private val serverHealthApi: ServerHealthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerHealthUiState())
    val uiState: StateFlow<ServerHealthUiState> = _uiState.asStateFlow()

    fun checkServer() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingServer = true,
                    serverCheckMessage = null
                )
            }

            val result = runCatching {
                val response = serverHealthApi.checkServer()
                if (!response.isSuccessful) {
                    error("\uC11C\uBC84 \uD655\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (${response.code()})")
                }
            }

            _uiState.update {
                it.copy(
                    isCheckingServer = false,
                    serverCheckMessage = result.fold(
                        onSuccess = { "\uC11C\uBC84\uC5D0 \uC5F0\uACB0\uB418\uC5C8\uC2B5\uB2C8\uB2E4." },
                        onFailure = { error -> "\uC11C\uBC84 \uC5F0\uACB0\uC744 \uD655\uC778\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4." }
                    )
                )
            }
        }
    }
}
