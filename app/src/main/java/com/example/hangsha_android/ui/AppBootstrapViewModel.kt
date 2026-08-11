package com.example.hangsha_android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            runCatching { categoryRepository.ensureCategoryCatalogLoaded() }
        }
        viewModelScope.launch {
            runCatching { userRepository.ensureOrganizationNamesLoaded() }
        }
    }
}
