package com.example.hangsha_android.ui.view.splash

data class SplashUiState(
    val navigationTarget: SplashNavigationTarget? = null
)

enum class SplashNavigationTarget {
    Calendar,
    Login
}
