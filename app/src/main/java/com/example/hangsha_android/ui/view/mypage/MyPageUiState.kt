package com.example.hangsha_android.ui.view.mypage

data class MyPageUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val interests: List<String> = emptyList(),
    val errorMessage: String? = null
)
