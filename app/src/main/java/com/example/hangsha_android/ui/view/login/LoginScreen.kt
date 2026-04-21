package com.example.hangsha_android.ui.view.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.components.CheckServerButton
import com.example.hangsha_android.ui.components.ClearGoogleLoginHistoryButton
import com.example.hangsha_android.ui.view.serverhealth.ServerHealthUiState

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onClearGoogleLoginHistoryClick: () -> Unit,
    onCheckServerClick: () -> Unit,
    loginUiState: LoginUiState,
    serverHealthUiState: ServerHealthUiState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(text = "Login")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Login")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onGoogleLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loginUiState.isAnyLoginLoading
            ) {
                if (loginUiState.isGoogleLoginLoading) {
                    GoogleLoginProgressIndicator(size = 18.dp)
                } else {
                    Text(text = "Continue with Google")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ClearGoogleLoginHistoryButton(
                onClick = onClearGoogleLoginHistoryClick,
                enabled = !loginUiState.isAnyLoginLoading && !loginUiState.isGoogleHistoryClearing
            )
            loginUiState.loginMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            CheckServerButton(
                onClick = onCheckServerClick,
                isLoading = serverHealthUiState.isCheckingServer
            )
            serverHealthUiState.serverCheckMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun GoogleLoginProgressIndicator(size: Dp) {
    CircularProgressIndicator(
        modifier = Modifier.height(size),
        strokeWidth = 2.dp
    )
}
