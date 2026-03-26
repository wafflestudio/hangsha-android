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
                    error("Server check failed with code ${response.code()}")
                }
            }

            _uiState.update {
                it.copy(
                    isCheckingServer = false,
                    serverCheckMessage = result.fold(
                        onSuccess = { "Server is reachable." },
                        onFailure = { error -> error.message ?: "Server check failed." }
                    )
                )
            }
        }
    }
}
