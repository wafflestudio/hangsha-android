package com.example.hangsha_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.hangsha_android.data.local.needsLocalDataReset
import com.example.hangsha_android.ui.HangshaApp
import com.example.hangsha_android.ui.theme.HangshaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        val shouldDiscardSavedState = needsLocalDataReset()
        super.onCreate(if (shouldDiscardSavedState) null else savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangshaTheme {
                HangshaApp()
            }
        }
    }
}
