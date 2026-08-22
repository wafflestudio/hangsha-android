package com.example.hangsha_android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    val catalogErrorMessage: StateFlow<String?> = categoryRepository.catalogErrorMessage

    init {
        viewModelScope.launch {
            runCatching { categoryRepository.ensureCategoryCatalogLoaded() }
        }
    }

    fun consumeCatalogError() {
        categoryRepository.consumeCatalogError()
    }
}
