package com.example.hangsha_android.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.hangsha_android.data.local.AuthTokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AuthStateViewModel @Inject constructor(
    authTokenStorage: AuthTokenStorage
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = authTokenStorage.isLoggedIn
}