package com.example.hangsha_android.ui.view.appupdate

data class AppUpdateUiState(
    val isCheckingPolicy: Boolean = false,
    val prompt: AppUpdatePromptUiState? = null,
    val toastMessage: String? = null
)

data class AppUpdatePromptUiState(
    val isRequired: Boolean,
    val latestVersionName: String,
    val releaseNotes: String
)
