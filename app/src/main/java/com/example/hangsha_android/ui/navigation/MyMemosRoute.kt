package com.example.hangsha_android.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.hangsha_android.ui.view.mymemos.MyMemosScreen
import com.example.hangsha_android.ui.view.mymemos.MyMemosViewModel

@Composable
internal fun MyMemosRoute(
    navController: NavHostController,
    onNavigateBack: (() -> Unit)?
) {
    val viewModel: MyMemosViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.toastMessage) {
        val message = uiState.toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.onToastMessageConsumed()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadMemos()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MyMemosScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onMemoClick = { eventId ->
            navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
        },
        onDeleteMemoClick = viewModel::deleteMemo,
        onStartEditMemo = viewModel::startEditMemo,
        onEditContentChanged = viewModel::onEditContentChanged,
        onStartAddingTag = viewModel::startAddingTag,
        onEditTagInputChanged = viewModel::onEditTagInputChanged,
        onAddEditTag = viewModel::addEditTag,
        onRemoveEditTag = viewModel::removeEditTag,
        onSaveEditedMemo = viewModel::saveEditedMemo,
        onRetryClick = viewModel::loadMemos
    )
}
