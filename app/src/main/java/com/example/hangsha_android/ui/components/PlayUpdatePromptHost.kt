package com.example.hangsha_android.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.ui.view.appupdate.AppUpdateViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

/** Google Play에서 새 빌드가 확인된 경우에만 서버 정책을 조회해 업데이트 안내를 띄운다. */
@Composable
fun PlayUpdatePromptHost(
    enabled: Boolean,
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val toastState = LocalHangshaToastState.current
    val uiState by viewModel.uiState.collectAsState()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentPolicyChecking by rememberUpdatedState(uiState.isCheckingPolicy)
    val updateManager = remember(context) { AppUpdateManagerFactory.create(context) }
    var dismissedThisForeground by remember { mutableStateOf(false) }
    var checkedThisForeground by remember { mutableStateOf(false) }
    var isCheckingPlay by remember { mutableStateOf(false) }
    var openingStore by remember { mutableStateOf(false) }
    var storeOpenedForRequiredUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.toastMessage) {
        val message = uiState.toastMessage ?: return@LaunchedEffect
        toastState.show(message, HangshaToastType.Error)
        viewModel.onToastMessageConsumed()
    }

    fun checkForUpdate(force: Boolean = false) {
        if (
            !currentEnabled ||
            dismissedThisForeground ||
            isCheckingPlay ||
            currentPolicyChecking ||
            (checkedThisForeground && !force)
        ) {
            return
        }

        checkedThisForeground = true
        isCheckingPlay = true
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                isCheckingPlay = false
                if (
                    !currentEnabled ||
                    !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) {
                    return@addOnSuccessListener
                }

                val availableVersionCode = info.availableVersionCode()
                if (
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    availableVersionCode > BuildConfig.VERSION_CODE
                ) {
                    viewModel.checkUpdatePolicy(availableVersionCode)
                } else {
                    viewModel.cancelCheckAndClearPrompt()
                }
            }
            .addOnFailureListener { error ->
                isCheckingPlay = false
                if (
                    currentEnabled &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) {
                    viewModel.reportPlayUpdateCheckFailure(error)
                }
            }
    }

    DisposableEffect(enabled, lifecycleOwner) {
        if (enabled) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        viewModel.cancelCheckAndClearPrompt()
                        if (!openingStore) {
                            dismissedThisForeground = false
                            checkedThisForeground = false
                        }
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        if (openingStore) {
                            openingStore = false
                            if (storeOpenedForRequiredUpdate) {
                                storeOpenedForRequiredUpdate = false
                                dismissedThisForeground = false
                                checkedThisForeground = false
                                checkForUpdate(force = true)
                            }
                        } else {
                            checkForUpdate()
                        }
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                checkForUpdate()
            }
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        } else {
            viewModel.cancelCheckAndClearPrompt()
            onDispose { }
        }
    }

    val prompt = uiState.prompt
    if (enabled && prompt != null && !dismissedThisForeground) {
        UpdatePromptDialog(
            isRequired = prompt.isRequired,
            latestVersionName = prompt.latestVersionName,
            releaseNotes = prompt.releaseNotes,
            onDismissRequest = {
                if (!prompt.isRequired) {
                    dismissedThisForeground = true
                    viewModel.cancelCheckAndClearPrompt()
                }
            },
            onUpdateClick = {
                openingStore = true
                storeOpenedForRequiredUpdate = prompt.isRequired
                if (openPlayStore(context)) {
                    if (!prompt.isRequired) dismissedThisForeground = true
                    viewModel.cancelCheckAndClearPrompt()
                } else {
                    openingStore = false
                    storeOpenedForRequiredUpdate = false
                    toastState.show(STORE_OPEN_FAILED_MESSAGE, HangshaToastType.Error)
                }
            }
        )
    }
}

private fun openPlayStore(context: Context): Boolean {
    val packageName = context.packageName
    val playIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).setPackage("com.android.vending")
    if (tryStartActivity(context, playIntent)) return true

    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    )
    return tryStartActivity(context, browserIntent)
}

private fun tryStartActivity(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private const val STORE_OPEN_FAILED_MESSAGE = "앱 스토어를 열지 못했어요."
