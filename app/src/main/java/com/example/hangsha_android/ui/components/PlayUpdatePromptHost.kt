package com.example.hangsha_android.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hangsha_android.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

/** Google Play에서 새 빌드가 확인되면 메인 화면 위에 선택 업데이트 안내를 띄운다. */
@Composable
fun PlayUpdatePromptHost(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentEnabled by rememberUpdatedState(enabled)
    val updateManager = remember(context) { AppUpdateManagerFactory.create(context) }
    var isPromptVisible by remember { mutableStateOf(false) }
    var dismissedThisForeground by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var openingStore by remember { mutableStateOf(false) }

    fun checkForUpdate() {
        if (!currentEnabled || dismissedThisForeground || isChecking || isPromptVisible) return
        isChecking = true
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                isChecking = false
                if (
                    currentEnabled &&
                    !dismissedThisForeground &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.availableVersionCode() > BuildConfig.VERSION_CODE
                ) {
                    isPromptVisible = true
                }
            }
            .addOnFailureListener { isChecking = false }
    }

    DisposableEffect(enabled, lifecycleOwner) {
        if (enabled) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        isPromptVisible = false
                        if (!openingStore) dismissedThisForeground = false
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        if (openingStore) {
                            openingStore = false
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
            isPromptVisible = false
            onDispose { }
        }
    }

    if (enabled && isPromptVisible) {
        UpdatePromptDialog(
            isRequired = false,
            onDismissRequest = {
                dismissedThisForeground = true
                isPromptVisible = false
            },
            onUpdateClick = {
                dismissedThisForeground = true
                isPromptVisible = false
                openingStore = true
                val packageName = context.packageName
                val playIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                ).setPackage("com.android.vending")
                try {
                    context.startActivity(playIntent)
                } catch (_: ActivityNotFoundException) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                    try {
                        context.startActivity(browserIntent)
                    } catch (_: ActivityNotFoundException) {
                        openingStore = false
                    }
                }
            }
        )
    }
}
